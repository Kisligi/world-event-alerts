import { NavLink, Navigate, Route, Routes } from 'react-router';
import { AlertsPage } from './pages/AlertsPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { useUsers } from './UserContext';

export function App() {
  return (
    <>
      <header className="topbar">
        <span className="brand">World Event Alerts</span>
        <nav>
          <NavLink to="/" end>
            My alerts
          </NavLink>
          <NavLink to="/notifications">Notifications</NavLink>
        </nav>
        <UserSwitcher />
      </header>
      <main>
        <Routes>
          <Route path="/" element={<AlertsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}

/** Stands in for a login (docs/decisions.md ADR-004). */
function UserSwitcher() {
  const { users, currentUser, selectUser } = useUsers();
  if (!currentUser) {
    return null; // still loading, or failed: the page shows the error
  }
  return (
    <label className="user-switcher">
      Acting as
      <select value={currentUser.id} onChange={(e) => selectUser(Number(e.target.value))}>
        {users.map((u) => (
          <option key={u.id} value={u.id}>
            {u.name}
          </option>
        ))}
      </select>
    </label>
  );
}
