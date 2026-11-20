import { useCallback, useEffect, useState } from 'react';

export interface ApiState<T> {
  data: T | undefined;
  error: Error | undefined;
  loading: boolean;
  reload: () => void;
}

/**
 * Loads data with {@code load} whenever {@code key} changes, and on {@code reload()}.
 * A response that arrives after the key changed (e.g. the user was switched) is ignored, so a
 * slow answer for the previous user can never overwrite the current one.
 */
export function useApi<T>(load: () => Promise<T>, key: unknown): ApiState<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<Error>();
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    load()
      .then((result) => {
        if (current) {
          setData(result);
          setError(undefined);
        }
      })
      .catch((e: unknown) => {
        if (current) {
          setError(e instanceof Error ? e : new Error(String(e)));
        }
      })
      .finally(() => {
        if (current) {
          setLoading(false);
        }
      });
    return () => {
      current = false;
    };
    // `load` is intentionally not a dependency: callers pass a new arrow function on every
    // render, and `key` says when the data to load actually changes.
  }, [key, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data, error, loading, reload };
}
