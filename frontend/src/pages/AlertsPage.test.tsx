import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ALICE, BOB, fakeApi, rule } from '../test/fakeApi';
import { renderApp } from '../test/renderApp';

const users = [ALICE, BOB];

describe('My alerts', () => {
  it("shows the first user's alerts by default", async () => {
    fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': [rule(), rule({ id: 11, name: 'Markets', enabled: false, keywords: [] })],
    });

    renderApp();

    const quakes = await screen.findByRole('article', { name: 'Major earthquakes' });
    expect(within(quakes).getByText('High or higher')).toBeInTheDocument();
    expect(within(quakes).getByText('alice@example.com')).toBeInTheDocument();
    const markets = screen.getByRole('article', { name: 'Markets' });
    expect(within(markets).getByText('Off')).toBeInTheDocument();
    expect(within(markets).getByText('any event')).toBeInTheDocument();
  });

  it('switches user, loads their alerts and remembers the choice', async () => {
    const backend = fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': [rule()],
      'GET /api/users/2/alerts': [],
    });
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole('article', { name: 'Major earthquakes' });

    await user.selectOptions(screen.getByRole('combobox', { name: 'Acting as' }), 'Bob Trader');

    expect(await screen.findByText(/You have no alerts yet/)).toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
    expect(backend.callsTo('GET /api/users/2/alerts')).toHaveLength(1);
    expect(localStorage.getItem('world-event-alerts.userId')).toBe('2');
  });

  it('starts with the remembered user', async () => {
    localStorage.setItem('world-event-alerts.userId', '2');
    const backend = fakeApi({ 'GET /api/users': users, 'GET /api/users/2/alerts': [] });

    renderApp();

    await screen.findByText(/You have no alerts yet/);
    expect(screen.getByRole('combobox', { name: 'Acting as' })).toHaveValue('2');
    expect(backend.callsTo('GET /api/users/1/alerts')).toHaveLength(0);
  });

  it('falls back to the first user if the remembered one no longer exists', async () => {
    localStorage.setItem('world-event-alerts.userId', '99');
    fakeApi({ 'GET /api/users': users, 'GET /api/users/1/alerts': [] });

    renderApp();

    await screen.findByText(/You have no alerts yet/);
    expect(screen.getByRole('combobox', { name: 'Acting as' })).toHaveValue('1');
  });

  it("creates an alert for the current user, pre-filled with the user's email, and reloads the list", async () => {
    let stored: ReturnType<typeof rule>[] = [];
    const backend = fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': () => stored,
      'POST /api/users/1/alerts': ({ body }) => {
        stored = [rule({ id: 12, ...(body as object) })];
        return { status: 201, body: stored[0] };
      },
    });
    const user = userEvent.setup();
    renderApp();
    await screen.findByText(/You have no alerts yet/);

    await user.click(screen.getByRole('button', { name: 'New alert' }));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Tsunamis');
    await user.click(screen.getByRole('button', { name: 'Create alert' }));

    expect(await screen.findByRole('article', { name: 'Tsunamis' })).toBeInTheDocument();
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
    expect(backend.callsTo('POST /api/users/1/alerts')[0].body).toMatchObject({
      name: 'Tsunamis',
      targets: [{ type: 'EMAIL', address: 'alice@example.com' }],
    });
  });

  it('closes the form without saving on Cancel', async () => {
    const backend = fakeApi({ 'GET /api/users': users, 'GET /api/users/1/alerts': [rule()] });
    const user = userEvent.setup();
    renderApp();
    const card = await screen.findByRole('article', { name: 'Major earthquakes' });

    await user.click(screen.getByRole('button', { name: 'New alert' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await user.click(within(card).getByRole('button', { name: 'Edit' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('form')).not.toBeInTheDocument();
    expect(screen.getByRole('article', { name: 'Major earthquakes' })).toBeInTheDocument();
    expect(backend.calls.filter((c) => c.method !== 'GET')).toHaveLength(0);
  });

  it('edits an alert with PUT and closes the form', async () => {
    let current = rule();
    const backend = fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': () => [current],
      'PUT /api/alerts/10': ({ body }) => (current = { ...current, ...(body as object) }),
    });
    const user = userEvent.setup();
    renderApp();
    const card = await screen.findByRole('article', { name: 'Major earthquakes' });

    await user.click(within(card).getByRole('button', { name: 'Edit' }));
    const name = screen.getByRole('textbox', { name: 'Name' });
    await user.clear(name);
    await user.type(name, 'Big quakes');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('article', { name: 'Big quakes' })).toBeInTheDocument();
    expect(backend.callsTo('PUT /api/alerts/10')[0].body).toMatchObject({ name: 'Big quakes' });
  });

  it('turns an alert off by sending the whole rule with enabled = false', async () => {
    let current = rule();
    const backend = fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': () => [current],
      'PUT /api/alerts/10': ({ body }) => (current = { ...current, ...(body as object) }),
    });
    const user = userEvent.setup();
    renderApp();
    const card = await screen.findByRole('article', { name: 'Major earthquakes' });

    await user.click(within(card).getByRole('button', { name: 'Turn off' }));

    expect(await within(card).findByText('Off')).toBeInTheDocument();
    expect(backend.callsTo('PUT /api/alerts/10')[0].body).toEqual({
      name: 'Major earthquakes',
      categories: ['DISASTER'],
      keywords: ['earthquake'],
      minSeverity: 'HIGH',
      targets: [{ type: 'EMAIL', address: 'alice@example.com' }],
      enabled: false,
    });
  });

  it('deletes only after confirmation', async () => {
    let stored = [rule()];
    const backend = fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': () => stored,
      'DELETE /api/alerts/10': () => {
        stored = [];
        return { status: 204 };
      },
    });
    const user = userEvent.setup();
    renderApp();
    const card = await screen.findByRole('article', { name: 'Major earthquakes' });

    await user.click(within(card).getByRole('button', { name: 'Delete' }));
    await user.click(within(card).getByRole('button', { name: 'Keep it' }));
    expect(backend.callsTo('DELETE /api/alerts/10')).toHaveLength(0);

    await user.click(within(card).getByRole('button', { name: 'Delete' }));
    await user.click(within(card).getByRole('button', { name: 'Yes, delete' }));

    await waitFor(() => expect(screen.queryByRole('article')).not.toBeInTheDocument());
    expect(backend.callsTo('DELETE /api/alerts/10')).toHaveLength(1);
  });

  it('shows why an action failed', async () => {
    fakeApi({
      'GET /api/users': users,
      'GET /api/users/1/alerts': [rule()],
      'DELETE /api/alerts/10': { status: 404, body: { detail: 'Alert rule 10 not found' } },
    });
    const user = userEvent.setup();
    renderApp();
    const card = await screen.findByRole('article', { name: 'Major earthquakes' });

    await user.click(within(card).getByRole('button', { name: 'Delete' }));
    await user.click(within(card).getByRole('button', { name: 'Yes, delete' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Alert rule 10 not found');
  });

  it('says so when the backend cannot be reached', async () => {
    fakeApi({}); // every request fails as an unexpected route
    renderApp();

    expect(await screen.findByRole('alert')).toHaveTextContent('The server cannot be reached. Is the backend running?');
    expect(screen.queryByRole('combobox', { name: 'Acting as' })).not.toBeInTheDocument();
  });

  it('sends unknown paths to My alerts', async () => {
    fakeApi({ 'GET /api/users': users, 'GET /api/users/1/alerts': [] });

    renderApp('/no-such-page');

    expect(await screen.findByRole('heading', { name: 'My alerts' })).toBeInTheDocument();
  });
});
