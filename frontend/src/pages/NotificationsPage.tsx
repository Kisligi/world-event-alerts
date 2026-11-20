import { api } from '../api/client';
import type { User } from '../api/types';
import { DeliveryTable } from '../components/DeliveryTable';
import { ErrorMessage } from '../components/ErrorMessage';
import { WithUser } from '../components/WithUser';
import { useApi } from '../useApi';
import { useAutoRefresh } from '../useAutoRefresh';

export function NotificationsPage() {
  return <WithUser>{(user) => <UserNotifications user={user} />}</WithUser>;
}

function UserNotifications({ user }: { user: User }) {
  const { data: deliveries, error, loading, reload } = useApi(() => api.listNotifications(user.id), user.id);
  useAutoRefresh(reload);

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
      {deliveries && deliveries.length > 0 && <DeliveryTable deliveries={deliveries} />}
    </section>
  );
}
