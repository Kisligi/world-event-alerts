import { useState } from 'react';
import type { AlertRule } from '../api/types';
import { categoryLabel, channelLabel, severityLabel } from '../format';

interface Props {
  rule: AlertRule;
  busy: boolean;
  onEdit: () => void;
  onToggle: () => void;
  onDelete: () => void;
}

/** One alert rule: what it listens to, where it notifies, and its actions. */
export function AlertCard({ rule, busy, onEdit, onToggle, onDelete }: Props) {
  const [confirming, setConfirming] = useState(false);

  return (
    <article className={`card alert-card${rule.enabled ? '' : ' disabled'}`} aria-label={rule.name}>
      <header>
        <h3>{rule.name}</h3>
        <span className={`badge ${rule.enabled ? 'on' : 'off'}`}>{rule.enabled ? 'On' : 'Off'}</span>
      </header>
      <dl>
        <dt>Categories</dt>
        <dd>{rule.categories.map(categoryLabel).join(', ')}</dd>
        <dt>Severity</dt>
        <dd>{severityLabel(rule.minSeverity)} or higher</dd>
        <dt>Keywords</dt>
        <dd>{rule.keywords.length ? rule.keywords.join(', ') : 'any event'}</dd>
        <dt>Notify</dt>
        <dd>
          <ul className="targets">
            {rule.targets.map((t) => (
              <li key={`${t.type}:${t.address}`}>
                <span className="channel">{channelLabel(t.type)}</span> {t.address}
              </li>
            ))}
          </ul>
        </dd>
      </dl>
      <div className="actions">
        {confirming ? (
          <>
            <span>Delete this alert?</span>
            <button type="button" className="danger" onClick={onDelete} disabled={busy}>
              Yes, delete
            </button>
            <button type="button" className="secondary" onClick={() => setConfirming(false)}>
              Keep it
            </button>
          </>
        ) : (
          <>
            <button type="button" className="secondary" onClick={onToggle} disabled={busy}>
              {rule.enabled ? 'Turn off' : 'Turn on'}
            </button>
            <button type="button" className="secondary" onClick={onEdit} disabled={busy}>
              Edit
            </button>
            <button type="button" className="secondary" onClick={() => setConfirming(true)} disabled={busy}>
              Delete
            </button>
          </>
        )}
      </div>
    </article>
  );
}
