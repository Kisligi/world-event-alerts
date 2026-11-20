import { Fragment, useState } from 'react';
import type { Delivery, DeliveryStatus } from '../api/types';
import { channelLabel, formatDateTime } from '../format';

const STATUS_TEXT: Record<DeliveryStatus, string> = {
  PENDING: 'Sending',
  SENT: 'Sent',
  FAILED: 'Failed',
  SKIPPED: 'Skipped',
};

interface Props {
  deliveries: Delivery[];
  /** Adds a "User" column (admin view): user id → name. */
  userNames?: Map<number, string>;
}

/** Deliveries newest first; selecting a row shows the exact message and the error. */
export function DeliveryTable({ deliveries, userNames }: Props) {
  const [openId, setOpenId] = useState<number>();
  const columns = userNames ? 6 : 5;

  return (
    <table className="notifications">
      <thead>
        <tr>
          <th>Time</th>
          <th>Status</th>
          {userNames && <th>User</th>}
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
              {userNames && <td>{userNames.get(d.userId) ?? `User ${d.userId}`}</td>}
              <td>{d.subject}</td>
              <td className="address">
                <span className="channel">{channelLabel(d.channelType)}</span> {d.address}
              </td>
              <td>{d.alertRuleName}</td>
            </tr>
            {openId === d.id && (
              <tr className="details">
                <td colSpan={columns}>
                  {d.lastError && <p className="error">{d.lastError}</p>}
                  <pre>{d.body}</pre>
                </td>
              </tr>
            )}
          </Fragment>
        ))}
      </tbody>
    </table>
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
