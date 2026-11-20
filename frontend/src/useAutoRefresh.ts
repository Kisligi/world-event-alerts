import { useEffect } from 'react';

/** New data appears without a page reload; the simulator emits an event every 15 s. */
export const REFRESH_INTERVAL_MS = 10_000;

/** Calls {@code reload} every {@link REFRESH_INTERVAL_MS} while the component is shown. */
export function useAutoRefresh(reload: () => void) {
  useEffect(() => {
    const timer = setInterval(reload, REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [reload]);
}
