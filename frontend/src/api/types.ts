// Types mirroring the backend's JSON (see docs/architecture.md, "REST API").

export const CATEGORIES = ['NEWS', 'MARKET', 'DISASTER'] as const;
export type Category = (typeof CATEGORIES)[number];

export const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
export type Severity = (typeof SEVERITIES)[number];

export const CHANNEL_TYPES = ['EMAIL', 'SLACK'] as const;
export type ChannelType = (typeof CHANNEL_TYPES)[number];

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface ChannelTarget {
  type: ChannelType;
  address: string;
}

export interface AlertRule {
  id: number;
  userId: number;
  name: string;
  categories: Category[];
  keywords: string[];
  minSeverity: Severity;
  targets: ChannelTarget[];
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Body of POST /api/users/{id}/alerts and PUT /api/alerts/{id}. */
export interface AlertRuleRequest {
  name: string;
  categories: Category[];
  keywords: string[];
  minSeverity: Severity;
  targets: ChannelTarget[];
  enabled: boolean;
}

export type DeliveryStatus = 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED';

export interface Delivery {
  id: number;
  eventId: number;
  alertRuleId: number | null;
  alertRuleName: string;
  userId: number;
  channelType: ChannelType;
  address: string;
  status: DeliveryStatus;
  attempts: number;
  lastError: string | null;
  subject: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export const DELIVERY_STATUSES: DeliveryStatus[] = ['SENT', 'FAILED', 'SKIPPED', 'PENDING'];

export interface WorldEvent {
  id: number;
  source: string;
  externalId: string | null;
  category: Category;
  severity: Severity;
  title: string;
  description: string | null;
  url: string | null;
  occurredAt: string;
  receivedAt: string;
}

/** Body of POST /api/admin/events. */
export interface PublishEventRequest {
  category: Category;
  severity: Severity;
  title: string;
  description?: string;
}

export interface IngestionResult {
  event: WorldEvent | null;
  duplicate: boolean;
  matchedAlertRuleIds: number[];
}

export interface ChannelStatus {
  type: ChannelType;
  enabled: boolean;
  /** False when the type exists but no channel implementation is deployed. */
  implemented: boolean;
}

