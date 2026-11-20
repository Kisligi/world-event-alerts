import { useState, type FormEvent } from 'react';
import {
  CATEGORIES,
  CHANNEL_TYPES,
  SEVERITIES,
  type AlertRule,
  type AlertRuleRequest,
  type Category,
  type ChannelTarget,
  type Severity,
} from '../api/types';
import { categoryLabel, channelLabel, parseKeywords, severityLabel } from '../format';
import { ErrorMessage } from './ErrorMessage';

/** The backend accepts at most this many targets per rule. */
const MAX_TARGETS = 10;

const PLACEHOLDERS = {
  EMAIL: 'you@example.com',
  SLACK: 'https://hooks.slack.com/services/... or http://localhost:8089/services/demo',
};

interface Props {
  /** The rule being edited; a new rule when absent. */
  rule?: AlertRule;
  /** Pre-filled email target for a new rule. */
  defaultEmail?: string;
  onSubmit: (request: AlertRuleRequest) => Promise<void>;
  onCancel: () => void;
}

/**
 * Create or edit an alert rule. Checks only what the user can fix at a glance (name, at least
 * one category, at least one target address); everything else is validated by the backend and
 * its errors are shown here.
 */
export function AlertForm({ rule, defaultEmail, onSubmit, onCancel }: Props) {
  const [name, setName] = useState(rule?.name ?? '');
  const [categories, setCategories] = useState<Category[]>(rule?.categories ?? ['DISASTER']);
  const [keywords, setKeywords] = useState(rule?.keywords.join(', ') ?? '');
  const [minSeverity, setMinSeverity] = useState<Severity>(rule?.minSeverity ?? 'HIGH');
  const [targets, setTargets] = useState<ChannelTarget[]>(
    rule?.targets ?? [{ type: 'EMAIL', address: defaultEmail ?? '' }],
  );
  const [enabled, setEnabled] = useState(rule?.enabled ?? true);
  const [problem, setProblem] = useState<string>();
  const [error, setError] = useState<Error>();
  const [saving, setSaving] = useState(false);

  const toggleCategory = (category: Category) =>
    setCategories((current) =>
      current.includes(category) ? current.filter((c) => c !== category) : [...current, category],
    );

  const updateTarget = (index: number, change: Partial<ChannelTarget>) =>
    setTargets((current) => current.map((t, i) => (i === index ? { ...t, ...change } : t)));

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const cleanTargets = targets.map((t) => ({ ...t, address: t.address.trim() }));
    const missing = !name.trim()
      ? 'Give the alert a name.'
      : categories.length === 0
        ? 'Choose at least one category.'
        : cleanTargets.length === 0 || cleanTargets.some((t) => !t.address)
          ? 'Every notification target needs an address.'
          : undefined;
    setProblem(missing);
    setError(undefined);
    if (missing) {
      return;
    }
    setSaving(true);
    try {
      await onSubmit({
        name: name.trim(),
        // Keep the order of the checkboxes, not the order they were clicked in.
        categories: CATEGORIES.filter((c) => categories.includes(c)),
        keywords: parseKeywords(keywords),
        minSeverity,
        targets: cleanTargets,
        enabled,
      });
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card alert-form" onSubmit={handleSubmit} noValidate aria-label="Alert rule">
      <h2>{rule ? `Edit “${rule.name}”` : 'New alert'}</h2>

      <label>
        Name
        <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} />
      </label>

      <fieldset>
        <legend>Categories</legend>
        {CATEGORIES.map((category) => (
          <label key={category} className="inline">
            <input
              type="checkbox"
              checked={categories.includes(category)}
              onChange={() => toggleCategory(category)}
            />
            {categoryLabel(category)}
          </label>
        ))}
      </fieldset>

      <label>
        Keywords <span className="hint">optional, comma-separated; any one must appear in the title or description</span>
        <input value={keywords} onChange={(e) => setKeywords(e.target.value)} placeholder="earthquake, Japan" />
      </label>

      <label>
        Minimum severity
        <select value={minSeverity} onChange={(e) => setMinSeverity(e.target.value as Severity)}>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>
              {severityLabel(s)}
            </option>
          ))}
        </select>
      </label>

      <fieldset>
        <legend>Notify me by</legend>
        {targets.map((target, index) => (
          <div className="target-row" key={index}>
            <select
              aria-label={`Channel ${index + 1}`}
              value={target.type}
              onChange={(e) => updateTarget(index, { type: e.target.value as ChannelTarget['type'] })}
            >
              {CHANNEL_TYPES.map((type) => (
                <option key={type} value={type}>
                  {channelLabel(type)}
                </option>
              ))}
            </select>
            <input
              aria-label={`Address ${index + 1}`}
              value={target.address}
              placeholder={PLACEHOLDERS[target.type]}
              onChange={(e) => updateTarget(index, { address: e.target.value })}
            />
            <button
              type="button"
              className="secondary"
              onClick={() => setTargets((current) => current.filter((_, i) => i !== index))}
              disabled={targets.length === 1}
              aria-label={`Remove target ${index + 1}`}
            >
              Remove
            </button>
          </div>
        ))}
        <button
          type="button"
          className="secondary"
          onClick={() => setTargets((current) => [...current, { type: 'SLACK', address: '' }])}
          disabled={targets.length >= MAX_TARGETS}
        >
          Add target
        </button>
      </fieldset>

      <label className="inline">
        <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        Enabled
      </label>

      {problem && (
        <p className="error" role="alert">
          {problem}
        </p>
      )}
      <ErrorMessage error={error} />

      <div className="actions">
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : rule ? 'Save changes' : 'Create alert'}
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
      </div>
    </form>
  );
}
