import { useMemo, useState } from 'react';
import { api } from '../../api/client';
import { DELIVERY_STATUSES, type DeliveryStatus } from '../../api/types';
import { DeliveryTable } from '../../components/DeliveryTable';
import { ErrorMessage } from '../../components/ErrorMessage';
import { useApi } from '../../useApi';
import { useAutoRefresh } from '../../useAutoRefresh';
import { useUsers } from '../../UserContext';

const FILTER_LABELS: Record<DeliveryStatus, string> = {
  SENT: 'Sent',
  FAILED: 'Failed',
  SKIPPED: 'Skipped',
  PENDING: 'Sending',
};

/** Every delivery attempt for every user, newest first, optionally only one status. */
export function DeliveryLogPage() {
  const [status, setStatus] = useState<DeliveryStatus>();
  const { data: deliveries, error, loading, reload } = useApi(() => api.listDeliveries(status), status ?? 'all');
  const { users } = useUsers();
  const userNames = useMemo(() => new Map(users.map((u) => [u.id, u.name])), [users]);
  useAutoRefresh(reload);

  return (
    <>
      <div className="page-header">
        <p className="lead">The latest 100 deliveries for all users. Select one to see the message and the error.</p>
        <label className="inline">
          Status
          <select
            value={status ?? ''}
            onChange={(e) => setStatus(e.target.value ? (e.target.value as DeliveryStatus) : undefined)}
          >
            <option value="">All</option>
            {DELIVERY_STATUSES.map((s) => (
              <option key={s} value={s}>
                {FILTER_LABELS[s]}
              </option>
            ))}
          </select>
        </label>
      </div>
      <ErrorMessage error={error} />
      {loading && !deliveries && <p>Loading deliveries…</p>}
      {deliveries && deliveries.length === 0 && <p className="empty">No deliveries{status ? ' with this status' : ''}.</p>}
      {deliveries && deliveries.length > 0 && <DeliveryTable deliveries={deliveries} userNames={userNames} />}
    </>
  );
}
