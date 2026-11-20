import type { AlertRule, AlertRuleRequest, Delivery, User } from './types';

/**
 * An error response from the backend. The backend always answers errors with an RFC 9457
 * problem detail ({@code application/problem+json}); validation errors add an {@code errors}
 * list of "field: message" strings.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: string[];

  constructor(status: number, message: string, fieldErrors: string[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'The server cannot be reached. Is the backend running?');
  }
  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as { detail?: string; title?: string; errors?: unknown };
    const fieldErrors = Array.isArray(problem.errors) ? problem.errors.map(String) : [];
    const message = problem.detail ?? problem.title ?? `Request failed with status ${response.status}`;
    return new ApiError(response.status, message, fieldErrors);
  } catch {
    return new ApiError(response.status, `Request failed with status ${response.status}`);
  }
}

export const api = {
  listUsers: () => request<User[]>('GET', '/api/users'),
  listAlerts: (userId: number) => request<AlertRule[]>('GET', `/api/users/${userId}/alerts`),
  createAlert: (userId: number, rule: AlertRuleRequest) =>
    request<AlertRule>('POST', `/api/users/${userId}/alerts`, rule),
  updateAlert: (id: number, rule: AlertRuleRequest) => request<AlertRule>('PUT', `/api/alerts/${id}`, rule),
  deleteAlert: (id: number) => request<void>('DELETE', `/api/alerts/${id}`),
  listNotifications: (userId: number, limit = 50) =>
    request<Delivery[]>('GET', `/api/users/${userId}/notifications?limit=${limit}`),
};

/** The request that would recreate this rule; used for edits and for the enable switch. */
export function toRequest(rule: AlertRule): AlertRuleRequest {
  return {
    name: rule.name,
    categories: rule.categories,
    keywords: rule.keywords,
    minSeverity: rule.minSeverity,
    targets: rule.targets,
    enabled: rule.enabled,
  };
}
