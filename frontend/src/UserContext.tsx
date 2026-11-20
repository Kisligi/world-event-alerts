import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { api } from './api/client';
import type { User } from './api/types';
import { useApi } from './useApi';

const STORAGE_KEY = 'world-event-alerts.userId';

interface UserContextValue {
  users: User[];
  currentUser: User | undefined;
  selectUser: (id: number) => void;
  error: Error | undefined;
}

const UserContext = createContext<UserContextValue | undefined>(undefined);

function storedUserId(): number | undefined {
  try {
    const value = Number(localStorage.getItem(STORAGE_KEY));
    return Number.isInteger(value) && value > 0 ? value : undefined;
  } catch {
    return undefined; // storage blocked: just start with the first user
  }
}

/**
 * There is no login (docs/decisions.md ADR-004): the user picks one of the seeded demo users.
 * The choice is remembered in the browser.
 */
export function UserProvider({ children }: { children: ReactNode }) {
  const { data: users = [], error } = useApi(api.listUsers, 'users');
  const [selectedId, setSelectedId] = useState<number | undefined>(storedUserId);

  const value = useMemo<UserContextValue>(() => {
    const currentUser = users.find((u) => u.id === selectedId) ?? users[0];
    return {
      users,
      currentUser,
      error,
      selectUser: (id: number) => {
        setSelectedId(id);
        try {
          localStorage.setItem(STORAGE_KEY, String(id));
        } catch {
          // not remembered, still switched
        }
      },
    };
  }, [users, selectedId, error]);

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}

export function useUsers(): UserContextValue {
  const value = useContext(UserContext);
  if (!value) {
    throw new Error('useUsers must be used inside <UserProvider>');
  }
  return value;
}
