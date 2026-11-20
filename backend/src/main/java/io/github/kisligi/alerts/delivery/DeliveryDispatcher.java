package io.github.kisligi.alerts.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.kisligi.alerts.alert.AlertRule;
import io.github.kisligi.alerts.alert.AlertRuleRepository;
import io.github.kisligi.alerts.alert.ChannelTarget;
import io.github.kisligi.alerts.channel.ChannelSettingService;
import io.github.kisligi.alerts.channel.Notification;
import io.github.kisligi.alerts.channel.NotificationChannel;
import io.github.kisligi.alerts.common.ChannelType;
import io.github.kisligi.alerts.event.WorldEvent;
import io.github.kisligi.alerts.event.WorldEventMatched;
import io.github.kisligi.alerts.event.WorldEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends notifications for matched events (docs/decisions.md ADR-007, ADR-008).
 *
 * <p>For every target of every matched rule: record a delivery (at most one per event, rule and
 * target), skip it if the admin switched the channel off, otherwise send it through the
 * channel with retries and record the outcome. Runs after the ingestion transaction commits,
 * so a rolled-back event never sends anything.
 */
@Service
public class DeliveryDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatcher.class);

    private final WorldEventRepository events;
    private final AlertRuleRepository rules;
    private final DeliveryRepository deliveries;
    private final ChannelSettingService channelSettings;
    private final Map<ChannelType, NotificationChannel> channels;
    private final NotificationFormatter formatter;
    private final DeliveryProperties properties;
    private final Sleeper sleeper;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    public DeliveryDispatcher(WorldEventRepository events, AlertRuleRepository rules, DeliveryRepository deliveries,
                              ChannelSettingService channelSettings, List<NotificationChannel> channels,
                              NotificationFormatter formatter, DeliveryProperties properties, Sleeper sleeper,
                              TransactionTemplate transactionTemplate) {
        this.events = events;
        this.rules = rules;
        this.deliveries = deliveries;
        this.channelSettings = channelSettings;
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::type, Function.identity()));
        this.formatter = formatter;
        this.properties = properties;
        this.sleeper = sleeper;
        // Every step runs in its own short transaction, and none is open during a network call,
        // so a delivery holds at most one database connection, and only briefly (ADR-023).
        // REQUIRES_NEW keeps this true even if the announcement is ever published inside a
        // transaction: after commit, that transaction's resources can still be bound to the
        // thread, and plain repository calls would join it and never commit.
        this.readTx = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.readTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** One message to send: everything needed, read inside a transaction. */
    record Planned(Long eventId, Long ruleId, String ruleName, Long userId, ChannelTarget target,
                   Notification notification) {
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onEventMatched(WorldEventMatched matched) {
        dispatch(matched);
    }

    /** @return the deliveries created by this call (duplicates are not included) */
    public List<Delivery> dispatch(WorldEventMatched matched) {
        List<Delivery> created = new ArrayList<>();
        for (Planned planned : plan(matched)) {
            Delivery delivery = createIfNew(planned);
            if (delivery != null) {
                created.add(deliver(delivery, planned));
            }
        }
        return created;
    }

    private List<Planned> plan(WorldEventMatched matched) {
        return readTx.execute(status -> {
            WorldEvent event = events.findById(matched.eventId()).orElse(null);
            if (event == null) {
                log.warn("Matched event {} no longer exists; nothing to send", matched.eventId());
                return List.<Planned>of();
            }
            List<Planned> plan = new ArrayList<>();
            // Rules deleted since matching are simply not returned here.
            for (AlertRule rule : rules.findAllById(matched.alertRuleIds())) {
                Notification notification = formatter.format(event, rule.getName());
                for (ChannelTarget target : rule.getTargets()) {
                    plan.add(new Planned(event.getId(), rule.getId(), rule.getName(), rule.getUser().getId(),
                            target, notification));
                }
            }
            return plan;
        });
    }

    /** Records the delivery, or returns null if this exact notification already exists. */
    private Delivery createIfNew(Planned p) {
        if (deliveries.existsByEventIdAndAlertRuleIdAndChannelTypeAndAddress(
                p.eventId(), p.ruleId(), p.target().type(), p.target().address())) {
            log.debug("Delivery for event {} rule {} {} already exists", p.eventId(), p.ruleId(), p.target());
            return null;
        }
        try {
            return writeTx.execute(status -> deliveries.save(new Delivery(p.eventId(), p.ruleId(), p.ruleName(),
                    p.userId(), p.target().type(), p.target().address(), p.notification().subject(),
                    p.notification().body())));
        } catch (DataIntegrityViolationException e) {
            // Someone else created it between the check and the insert: still a duplicate.
            return null;
        }
    }

    private Delivery deliver(Delivery delivery, Planned p) {
        ChannelType type = delivery.getChannelType();
        if (!channelSettings.isEnabled(type)) {
            delivery.markSkipped("Channel " + type + " is switched off by the admin");
        } else if (!channels.containsKey(type)) {
            delivery.markFailed(0, "No channel implementation for " + type);
        } else {
            sendWithRetries(delivery, channels.get(type), p.notification());
        }
        log.info("Delivery {} for event {} via {} to {}: {} after {} attempt(s)", delivery.getId(),
                delivery.getEventId(), type, delivery.getAddress(), delivery.getStatus(), delivery.getAttempts());
        return writeTx.execute(status -> deliveries.save(delivery));
    }

    private void sendWithRetries(Delivery delivery, NotificationChannel channel, Notification notification) {
        String lastError = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            if (attempt > 1 && !waitBefore(attempt)) {
                delivery.markFailed(attempt - 1, lastError + " (retries interrupted)");
                return;
            }
            try {
                channel.send(delivery.getAddress(), notification);
                delivery.markSent(attempt);
                return;
            } catch (RuntimeException e) {
                // Some exceptions have no message; the type is better than an empty log entry.
                lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.debug("Attempt {} for delivery {} failed: {}", attempt, delivery.getId(), lastError);
            }
        }
        delivery.markFailed(properties.maxAttempts(), lastError);
    }

    private boolean waitBefore(int attempt) {
        try {
            sleeper.sleep(properties.backoffBefore(attempt));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
