import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useApi } from './useApi';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('useApi', () => {
  it('ignores a slow answer for a previous key', async () => {
    const forAlice = deferred<string>();
    const forBob = deferred<string>();
    const { result, rerender } = renderHook(({ key }) => useApi(() => (key === 1 ? forAlice : forBob).promise, key), {
      initialProps: { key: 1 },
    });

    rerender({ key: 2 });
    await act(async () => forBob.resolve("Bob's data"));
    await act(async () => forAlice.resolve("Alice's data (late)"));

    expect(result.current.data).toBe("Bob's data");
    expect(result.current.loading).toBe(false);
  });

  it('ignores a late failure for a previous key', async () => {
    const forAlice = deferred<string>();
    const { result, rerender } = renderHook(
      ({ key }) => useApi(() => (key === 1 ? forAlice.promise : Promise.resolve("Bob's data")), key),
      { initialProps: { key: 1 } },
    );

    rerender({ key: 2 });
    await waitFor(() => expect(result.current.data).toBe("Bob's data"));
    await act(async () => forAlice.reject(new Error('Alice request failed')));

    expect(result.current.error).toBeUndefined();
  });

  it('reports a failure, then clears it after a successful reload', async () => {
    let attempt = 0;
    const { result } = renderHook(() =>
      useApi(() => (++attempt === 1 ? Promise.reject(new Error('down')) : Promise.resolve('ok')), 'k'),
    );
    await waitFor(() => expect(result.current.error?.message).toBe('down'));

    act(() => result.current.reload());

    await waitFor(() => expect(result.current.data).toBe('ok'));
    expect(result.current.error).toBeUndefined();
  });

  it('wraps a non-Error rejection', async () => {
    const { result } = renderHook(() => useApi(() => Promise.reject('plain string'), 'k'));

    await waitFor(() => expect(result.current.error?.message).toBe('plain string'));
  });
});
