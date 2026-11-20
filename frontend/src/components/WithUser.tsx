import type { ReactNode } from 'react';
import type { User } from '../api/types';
import { useUsers } from '../UserContext';
import { ErrorMessage } from './ErrorMessage';

/**
 * Renders a page for the current user once the users are loaded. Keyed by user, so switching
 * users starts the page afresh (no open form or error carried over).
 */
export function WithUser({ children }: { children: (user: User) => ReactNode }) {
  const { currentUser, error } = useUsers();
  if (error) {
    return <ErrorMessage error={error} />;
  }
  if (!currentUser) {
    return <p>Loading…</p>;
  }
  return <div key={currentUser.id}>{children(currentUser)}</div>;
}
