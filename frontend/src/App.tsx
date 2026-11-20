import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router';
import { AdminLayout } from './pages/admin/AdminLayout';
import { ChannelsPage } from './pages/admin/ChannelsPage';
import { DeliveryLogPage } from './pages/admin/DeliveryLogPage';
import { EventsPage } from './pages/admin/EventsPage';
import { UsersAlertsPage } from './pages/admin/UsersAlertsPage';
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
          <NavLink to="/admin">Admin</NavLink>
        </nav>
        <UserSwitcher />
      </header>
      <main>
        <Routes>
          <Route path="/" element={<AlertsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<UsersAlertsPage />} />
            <Route path="events" element={<EventsPage />} />
            <Route path="deliveries" element={<DeliveryLogPage />} />
            <Route path="channels" element={<ChannelsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}

/** Stands in for a login (docs/decisions.md ADR-004). Not shown in the admin area, which covers all users. */
function UserSwitcher() {
  const { users, currentUser, selectUser } = useUsers();
  const { pathname } = useLocation();
  if (!currentUser || pathname.startsWith('/admin')) {
    return null; // still loading, failed (the page shows the error), or admin
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
