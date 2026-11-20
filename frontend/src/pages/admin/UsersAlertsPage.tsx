import { api } from '../../api/client';
import type { AlertRule, User } from '../../api/types';
import { ErrorMessage } from '../../components/ErrorMessage';
import { categoryLabel, channelLabel, severityLabel } from '../../format';
import { useApi } from '../../useApi';

async function load(): Promise<{ users: User[]; rules: AlertRule[] }> {
  const [users, rules] = await Promise.all([api.listUsers(), api.listAllAlerts()]);
  return { users, rules };
}

/** Every user with their alert rules, read-only (admin editing is trade-off T4). */
export function UsersAlertsPage() {
  const { data, error, loading } = useApi(load, 'users-and-alerts');

  return (
    <>
      <p className="lead">All demo users and their alert rules. Users manage their own rules on My alerts.</p>
      <ErrorMessage error={error} />
      {loading && !data && <p>Loading…</p>}
      {data?.users.map((user) => {
        const rules = data.rules.filter((r) => r.userId === user.id);
        return (
          <article key={user.id} className="card user-block" aria-label={user.name}>
            <header>
              <h2>{user.name}</h2>
              <span className="hint">
                {user.email} · {rules.length} {rules.length === 1 ? 'alert' : 'alerts'}
              </span>
            </header>
            {rules.length === 0 ? (
              <p className="empty">No alerts.</p>
            ) : (
              <table className="compact">
                <thead>
                  <tr>
                    <th>Alert</th>
                    <th>Categories</th>
                    <th>Severity</th>
                    <th>Keywords</th>
                    <th>Notifies</th>
                    <th>State</th>
                  </tr>
                </thead>
                <tbody>
                  {rules.map((rule) => (
                    <tr key={rule.id}>
                      <td>{rule.name}</td>
                      <td>{rule.categories.map(categoryLabel).join(', ')}</td>
                      <td>{severityLabel(rule.minSeverity)}+</td>
                      <td>{rule.keywords.length ? rule.keywords.join(', ') : 'any'}</td>
                      <td className="address">
                        {rule.targets.map((t) => (
                          <div key={`${t.type}:${t.address}`}>
                            <span className="channel">{channelLabel(t.type)}</span> {t.address}
                          </div>
                        ))}
                      </td>
                      <td>
                        <span className={`badge ${rule.enabled ? 'on' : 'off'}`}>{rule.enabled ? 'On' : 'Off'}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </article>
        );
      })}
    </>
  );
}
