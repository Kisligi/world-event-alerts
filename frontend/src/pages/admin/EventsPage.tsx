import { useState, type FormEvent } from 'react';
import { api } from '../../api/client';
import {
  CATEGORIES,
  SEVERITIES,
  type Category,
  type IngestionResult,
  type Severity,
} from '../../api/types';
import { ErrorMessage } from '../../components/ErrorMessage';
import { categoryLabel, formatDateTime, severityLabel } from '../../format';
import { useApi } from '../../useApi';
import { useAutoRefresh } from '../../useAutoRefresh';

export function EventsPage() {
  const { data: events, error, loading, reload } = useApi(() => api.listEvents(), 'events');
  useAutoRefresh(reload);

  return (
    <>
      <PublishEventForm onPublished={reload} />
      <h2>Recent events</h2>
      <p className="lead">The latest 50 events from every source, newest first.</p>
      <ErrorMessage error={error} />
      {loading && !events && <p>Loading events…</p>}
      {events && events.length === 0 && <p className="empty">No events yet.</p>}
      {events && events.length > 0 && (
        <table className="notifications">
          <thead>
            <tr>
              <th>Received</th>
              <th>Source</th>
              <th>Category</th>
              <th>Severity</th>
              <th>Title</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <tr key={e.id}>
                <td>{formatDateTime(e.receivedAt)}</td>
                <td>{e.source}</td>
                <td>{categoryLabel(e.category)}</td>
                <td>
                  <span className={`badge severity-${e.severity.toLowerCase()}`}>{severityLabel(e.severity)}</span>
                </td>
                <td>
                  {e.url ? (
                    <a href={e.url} target="_blank" rel="noreferrer">
                      {e.title}
                    </a>
                  ) : (
                    e.title
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}

/** Publishes an event by hand; it goes through the same matching and delivery as real ones. */
function PublishEventForm({ onPublished }: { onPublished: () => void }) {
  const [category, setCategory] = useState<Category>('DISASTER');
  const [severity, setSeverity] = useState<Severity>('CRITICAL');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [result, setResult] = useState<IngestionResult>();
  const [error, setError] = useState<Error>();
  const [sending, setSending] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setResult(undefined);
    setError(undefined);
    if (!title.trim()) {
      setError(new Error('Give the event a title.'));
      return;
    }
    setSending(true);
    try {
      setResult(
        await api.publishEvent({
          category,
          severity,
          title: title.trim(),
          description: description.trim() || undefined,
        }),
      );
      setTitle('');
      setDescription('');
      onPublished();
    } catch (err) {
      setError(err as Error);
    } finally {
      setSending(false);
    }
  }

  const matched = result?.matchedAlertRuleIds.length ?? 0;
  return (
    <form className="card publish-form" onSubmit={handleSubmit} noValidate aria-label="Publish test event">
      <h2>Publish a test event</h2>
      <p className="hint">
        Goes through the normal matching and delivery: matching alerts notify their users right away. Try a title
        with a keyword one of the alerts listens for.
      </p>
      <div className="form-row">
        <label>
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {categoryLabel(c)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Severity
          <select value={severity} onChange={(e) => setSeverity(e.target.value as Severity)}>
            {SEVERITIES.map((s) => (
              <option key={s} value={s}>
                {severityLabel(s)}
              </option>
            ))}
          </select>
        </label>
      </div>
      <label>
        Title
        <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={300} placeholder="M 7.2 earthquake near Tokyo" />
      </label>
      <label>
        Description <span className="hint">optional</span>
        <input value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} />
      </label>
      <ErrorMessage error={error} />
      {result && (
        <p className="success" role="status">
          Published event {result.event?.id}.{' '}
          {matched === 0
            ? 'It matched no alerts, so nobody was notified.'
            : `It matched ${matched} ${matched === 1 ? 'alert' : 'alerts'}; see the delivery log for the notifications.`}
        </p>
      )}
      <div className="actions">
        <button type="submit" disabled={sending}>
          {sending ? 'Publishing…' : 'Publish event'}
        </button>
      </div>
    </form>
  );
}
