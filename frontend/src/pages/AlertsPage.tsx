import { useState } from 'react';
import { api, toRequest } from '../api/client';
import type { AlertRule, AlertRuleRequest, User } from '../api/types';
import { AlertCard } from '../components/AlertCard';
import { AlertForm } from '../components/AlertForm';
import { ErrorMessage } from '../components/ErrorMessage';
import { WithUser } from '../components/WithUser';
import { useApi } from '../useApi';

/** "new" while creating, a rule id while editing that rule, undefined otherwise. */
type Editing = 'new' | number | undefined;

export function AlertsPage() {
  return <WithUser>{(user) => <UserAlerts user={user} />}</WithUser>;
}

function UserAlerts({ user }: { user: User }) {
  const { data: rules, error: loadError, loading, reload } = useApi(() => api.listAlerts(user.id), user.id);
  const [editing, setEditing] = useState<Editing>();
  const [busyId, setBusyId] = useState<number>();
  const [actionError, setActionError] = useState<Error>();

  async function save(request: AlertRuleRequest) {
    if (editing === 'new') {
      await api.createAlert(user.id, request);
    } else if (editing !== undefined) {
      await api.updateAlert(editing, request);
    }
    setEditing(undefined);
    reload();
  }

  async function act(rule: AlertRule, action: () => Promise<unknown>) {
    setBusyId(rule.id);
    setActionError(undefined);
    try {
      await action();
      reload();
    } catch (e) {
      setActionError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setBusyId(undefined);
    }
  }

  return (
    <section>
      <div className="page-header">
        <h1>My alerts</h1>
        {editing === undefined && (
          <button type="button" onClick={() => setEditing('new')}>
            New alert
          </button>
        )}
      </div>
      <p className="lead">
        Get an email or a Slack message when a news, market or disaster event matches one of your alerts.
      </p>

      <ErrorMessage error={loadError ?? actionError} />

      {editing === 'new' && (
        <AlertForm defaultEmail={user.email} onSubmit={save} onCancel={() => setEditing(undefined)} />
      )}

      {loading && !rules && <p>Loading alerts…</p>}
      {rules && rules.length === 0 && editing !== 'new' && (
        <p className="empty">You have no alerts yet. Create one to start getting notified.</p>
      )}

      <div className="grid">
        {rules?.map((rule) =>
          editing === rule.id ? (
            <AlertForm key={rule.id} rule={rule} onSubmit={save} onCancel={() => setEditing(undefined)} />
          ) : (
            <AlertCard
              key={rule.id}
              rule={rule}
              busy={busyId === rule.id}
              onEdit={() => setEditing(rule.id)}
              onToggle={() => act(rule, () => api.updateAlert(rule.id, { ...toRequest(rule), enabled: !rule.enabled }))}
              onDelete={() => act(rule, () => api.deleteAlert(rule.id))}
            />
          ),
        )}
      </div>
    </section>
  );
}
