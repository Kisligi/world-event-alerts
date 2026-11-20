import type { Category, ChannelType, Severity } from './api/types';

const CATEGORY_LABELS: Record<Category, string> = { NEWS: 'News', MARKET: 'Market', DISASTER: 'Disaster' };
const CHANNEL_LABELS: Record<ChannelType, string> = { EMAIL: 'Email', SLACK: 'Slack' };
const SEVERITY_LABELS: Record<Severity, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
};

export const categoryLabel = (category: Category) => CATEGORY_LABELS[category];
export const channelLabel = (type: ChannelType) => CHANNEL_LABELS[type];
export const severityLabel = (severity: Severity) => SEVERITY_LABELS[severity];

/** "20 Nov 2026, 14:09" in the browser's time zone. */
export function formatDateTime(iso: string, locale?: string): string {
  return new Intl.DateTimeFormat(locale ?? 'en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso));
}

/** "earthquake, Japan ,, tsunami" → ["earthquake", "Japan", "tsunami"]; the backend lower-cases. */
export function parseKeywords(text: string): string[] {
  const seen = new Set<string>();
  const result: string[] = [];
  for (const part of text.split(',')) {
    const keyword = part.trim();
    if (keyword && !seen.has(keyword.toLowerCase())) {
      seen.add(keyword.toLowerCase());
      result.push(keyword);
    }
  }
  return result;
}
