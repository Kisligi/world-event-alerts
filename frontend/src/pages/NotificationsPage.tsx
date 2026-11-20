import { Fragment, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Delivery, DeliveryStatus, User } from '../api/types';
import { ErrorMessage } from '../components/ErrorMessage';
import { WithUser } from '../components/WithUser';
import { channelLabel, formatDateTime } from '../format';
import { useApi } from '../useApi';

/** New notifications appear without a page reload; the simulator emits an event every 15 s. */
export const REFRESH_INTERVAL_MS = 10_000;

const STATUS_TEXT: Record<DeliveryStatus, string> = {
  PENDING: 'Sending',
  SENT: 'Sent',
  FAILED: 'Failed',
  SKIPPED: 'Skipped',
};

export function NotificationsPage() {
  return <WithUser>{(user) => <UserNotifications user={user} />}</WithUser>;
}

function UserNotifications({ user }: { user: User }) {
  const { data: deliveries, error, loading, reload } = useApi(() => api.listNotifications(user.id), user.id);
  const [openId, setOpenId] = useState<number>();

  useEffect(() => {
    const timer = setInterval(reload, REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [reload]);

  return (
    <section>
      <div className="page-header">
        <h1>Notifications</h1>
        <button type="button" className="secondary" onClick={reload}>
          Refresh
        </button>
      </div>
      <p className="lead">Every notification your alerts produced, newest first. Select one to see the message.</p>

      <ErrorMessage error={error} />
      {loading && !deliveries && <p>Loading notifications…</p>}
      {deliveries && deliveries.length === 0 && (
        <p className="empty">No notifications yet. They appear here as soon as an event matches one of your alerts.</p>
      )}

      {deliveries && deliveries.length > 0 && (
        <table className="notifications">
          <thead>
            <tr>
              <th>Time</th>
              <th>Status</th>
              <th>Message</th>
              <th>Sent to</th>
              <th>Alert</th>
            </tr>
          </thead>
          <tbody>
            {deliveries.map((d) => (
              <Fragment key={d.id}>
                <tr
                  className="clickable"
                  onClick={() => setOpenId(openId === d.id ? undefined : d.id)}
                  aria-expanded={openId === d.id}
                >
                  <td>{formatDateTime(d.createdAt)}</td>
                  <td>
                    <StatusBadge delivery={d} />
                  </td>
                  <td>{d.subject}</td>
                  <td>
                    <span className="channel">{channelLabel(d.channelType)}</span> {d.address}
                  </td>
                  <td>{d.alertRuleName}</td>
                </tr>
                {openId === d.id && (
                  <tr className="details">
                    <td colSpan={5}>
                      {d.lastError && <p className="error">{d.lastError}</p>}
                      <pre>{d.body}</pre>
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function StatusBadge({ delivery }: { delivery: Delivery }) {
  const retries = delivery.attempts > 1 ? ` after ${delivery.attempts} attempts` : '';
  return (
    <span className={`badge status-${delivery.status.toLowerCase()}`} title={delivery.lastError ?? undefined}>
      {STATUS_TEXT[delivery.status]}
      {retries}
    </span>
  );
}
