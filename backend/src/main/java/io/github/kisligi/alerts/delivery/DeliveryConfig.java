package io.github.kisligi.alerts.delivery;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DeliveryProperties.class)
public class DeliveryConfig {

    @Bean
    Sleeper sleeper() {
        return Sleeper.THREAD_SLEEP;
    }
}
