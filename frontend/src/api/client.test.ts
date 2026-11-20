import { describe, expect, it, vi } from 'vitest';
import { fakeApi, rule } from '../test/fakeApi';
import { ApiError, api, toRequest } from './client';

describe('api client', () => {
  it('sends JSON and returns the parsed response', async () => {
    const created = rule({ id: 42 });
    const backend = fakeApi({ 'POST /api/users/1/alerts': { status: 201, body: created } });

    const result = await api.createAlert(1, toRequest(created));

    expect(result).toEqual(created);
    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(init?.headers).toEqual({ 'Content-Type': 'application/json' });
    expect(backend.calls[0].body).toEqual(toRequest(created));
  });

  it('treats 204 No Content as success without a body', async () => {
    fakeApi({ 'DELETE /api/alerts/7': { status: 204 } });

    await expect(api.deleteAlert(7)).resolves.toBeUndefined();
  });

  it('turns a problem detail into an ApiError with its detail and field errors', async () => {
    fakeApi({
      'POST /api/users/1/alerts': {
        status: 400,
        body: { title: 'Bad Request', detail: 'Request validation failed', errors: ['name: must not be blank'] },
      },
    });

    const error = await api.createAlert(1, toRequest(rule())).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      message: 'Request validation failed',
      fieldErrors: ['name: must not be blank'],
    });
  });

  it('falls back to the title, then to the status, when there is no detail or no JSON', async () => {
    fakeApi({ 'GET /api/users': { status: 503, body: { title: 'Service Unavailable' } } });
    await expect(api.listUsers()).rejects.toThrow('Service Unavailable');

    vi.mocked(fetch).mockResolvedValueOnce(new Response('{}', { status: 500 }));
    await expect(api.listUsers()).rejects.toThrow('Request failed with status 500');

    vi.mocked(fetch).mockResolvedValueOnce(new Response('<html>proxy error</html>', { status: 502 }));
    await expect(api.listUsers()).rejects.toThrow('Request failed with status 502');
  });

  it('reports an unreachable backend in plain words', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

    const error = await api.listUsers().catch((e: unknown) => e);

    expect(error).toMatchObject({ status: 0, message: 'The server cannot be reached. Is the backend running?' });
  });

  it('asks for a limited notification history', async () => {
    const backend = fakeApi({ 'GET /api/users/2/notifications': [] });

    await api.listNotifications(2);

    expect(backend.calls[0].url).toBe('/api/users/2/notifications?limit=50');
  });

  it('builds an update request from a rule, without id, owner or timestamps', () => {
    expect(toRequest(rule({ enabled: false }))).toEqual({
      name: 'Major earthquakes',
      categories: ['DISASTER'],
      keywords: ['earthquake'],
      minSeverity: 'HIGH',
      targets: [{ type: 'EMAIL', address: 'alice@example.com' }],
      enabled: false,
    });
  });
});
