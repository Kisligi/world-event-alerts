import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { App } from '../App';
import { UserProvider } from '../UserContext';

/** The whole app, as main.tsx mounts it, at the given path. */
export function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <UserProvider>
        <App />
      </UserProvider>
    </MemoryRouter>,
  );
}
