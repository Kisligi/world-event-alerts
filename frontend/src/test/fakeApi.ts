import { vi } from 'vitest';
import type { AlertRule, Delivery, User } from '../api/types';

/** What a fake endpoint answers: a JSON body (status 200), or `{ status, body }`. */
export type Reply = object;

type Handler = (request: { method: string; url: string; body: unknown }) => Reply;

export interface FakeApi {
  /** Every request made, in order, e.g. "POST /api/users/1/alerts". */
  calls: { method: string; url: string; body: unknown }[];
  /** Requests matching "METHOD /path" (query string ignored). */
  callsTo: (route: string) => { method: string; url: string; body: unknown }[];
}

function isFullReply(reply: unknown): reply is { status: number; body?: unknown } {
  return typeof reply === 'object' && reply !== null && 'status' in reply && typeof reply.status === 'number';
}

/**
 * Replaces fetch with fake backend routes keyed by "METHOD /path" (query string ignored).
 * An unknown route fails the test loudly instead of hanging.
 */
export function fakeApi(routes: Record<string, Reply | Handler>): FakeApi {
  const calls: FakeApi['calls'] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    const body = typeof init?.body === 'string' ? JSON.parse(init.body) : undefined;
    calls.push({ method, url, body });
    const route = `${method} ${url.split('?')[0]}`;
    if (!(route in routes)) {
      throw new Error(`Unexpected request in test: ${route}`);
    }
    const handler = routes[route];
    const reply = typeof handler === 'function' ? (handler as Handler)({ method, url, body }) : handler;
    const { status, body: replyBody } = isFullReply(reply) ? reply : { status: 200, body: reply };
    return new Response(status === 204 ? null : JSON.stringify(replyBody), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    });
  });
  return {
    calls,
    callsTo: (route) => calls.filter((c) => `${c.method} ${c.url.split('?')[0]}` === route),
  };
}

export const ALICE: User = { id: 1, name: 'Alice Analyst', email: 'alice@example.com' };
export const BOB: User = { id: 2, name: 'Bob Trader', email: 'bob@example.com' };

export function rule(overrides: Partial<AlertRule> = {}): AlertRule {
  return {
    id: 10,
    userId: 1,
    name: 'Major earthquakes',
    categories: ['DISASTER'],
    keywords: ['earthquake'],
    minSeverity: 'HIGH',
    targets: [{ type: 'EMAIL', address: 'alice@example.com' }],
    enabled: true,
    createdAt: '2026-11-20T13:00:00Z',
    updatedAt: '2026-11-20T13:00:00Z',
    ...overrides,
  };
}

export function delivery(overrides: Partial<Delivery> = {}): Delivery {
  return {
    id: 100,
    eventId: 5,
    alertRuleId: 10,
    alertRuleName: 'Major earthquakes',
    userId: 1,
    channelType: 'EMAIL',
    address: 'alice@example.com',
    status: 'SENT',
    attempts: 1,
    lastError: null,
    subject: '[CRITICAL] Disaster: M 7.2 earthquake near Tokyo',
    body: 'M 7.2 earthquake near Tokyo\n\nCategory: Disaster',
    createdAt: '2026-11-20T13:28:00Z',
    updatedAt: '2026-11-20T13:28:01Z',
    ...overrides,
  };
}
