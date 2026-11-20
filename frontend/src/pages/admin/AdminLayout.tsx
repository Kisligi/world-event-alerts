import { NavLink, Outlet } from 'react-router';

/**
 * The admin area (docs/decisions.md ADR-005): monitor everything and test the pipeline.
 * Open to anyone, like the rest of the app: there is no login (ADR-004, trade-off T3).
 */
export function AdminLayout() {
  return (
    <section>
      <h1>Admin</h1>
      <nav className="subnav" aria-label="Admin">
        <NavLink to="/admin" end>
          Users and alerts
        </NavLink>
        <NavLink to="/admin/events">Events</NavLink>
        <NavLink to="/admin/deliveries">Delivery log</NavLink>
        <NavLink to="/admin/channels">Channels</NavLink>
      </nav>
      <Outlet />
    </section>
  );
}
