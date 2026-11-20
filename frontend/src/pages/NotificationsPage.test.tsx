import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ALICE, BOB, delivery, fakeApi } from '../test/fakeApi';
import { renderApp } from '../test/renderApp';
import { REFRESH_INTERVAL_MS } from './NotificationsPage';

describe('Notifications', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("lists the user's notifications with status, channel and alert", async () => {
    fakeApi({
      'GET /api/users': [ALICE],
      'GET /api/users/1/notifications': [
        delivery(),
        delivery({
          id: 101,
          channelType: 'SLACK',
          address: 'http://localhost:8089/services/fail/x',
          status: 'FAILED',
          attempts: 3,
          lastError: 'Slack webhook call failed: 500 Internal Server Error',
        }),
        delivery({ id: 102, status: 'SKIPPED', lastError: 'Channel EMAIL is switched off by the admin' }),
      ],
    });

    renderApp('/notifications');

    const rows = await screen.findAllByRole('row');
    expect(rows).toHaveLength(4); // header + 3
    expect(within(rows[1]).getByText('Sent')).toBeInTheDocument();
    expect(within(rows[1]).getByText('20 Nov 2026, 13:28')).toBeInTheDocument();
    expect(within(rows[1]).getByText('[CRITICAL] Disaster: M 7.2 earthquake near Tokyo')).toBeInTheDocument();
    expect(within(rows[2]).getByText('Failed after 3 attempts')).toBeInTheDocument();
    expect(within(rows[2]).getByText('Slack')).toBeInTheDocument();
    expect(within(rows[3]).getByText('Skipped')).toHaveAttribute('title', 'Channel EMAIL is switched off by the admin');
  });

  it('shows the message and the error when a row is selected, and hides it again', async () => {
    fakeApi({
      'GET /api/users': [ALICE],
      'GET /api/users/1/notifications': [delivery({ status: 'FAILED', attempts: 3, lastError: 'SMTP send failed' })],
    });
    const user = userEvent.setup();
    renderApp('/notifications');
    const row = (await screen.findAllByRole('row'))[1];

    await user.click(row);

    expect(row).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('SMTP send failed')).toBeInTheDocument();
    expect(screen.getByText(/Category: Disaster/)).toBeInTheDocument();

    await user.click(row);
    expect(screen.queryByText(/Category: Disaster/)).not.toBeInTheDocument();
  });

  it('explains an empty history', async () => {
    fakeApi({ 'GET /api/users': [ALICE], 'GET /api/users/1/notifications': [] });

    renderApp('/notifications');

    expect(await screen.findByText(/No notifications yet/)).toBeInTheDocument();
  });

  it('picks up new notifications by itself', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let history = [delivery()];
    const backend = fakeApi({
      'GET /api/users': [ALICE],
      'GET /api/users/1/notifications': () => history,
    });
    renderApp('/notifications');
    await screen.findByText('Sent');

    history = [delivery({ id: 103, subject: '[HIGH] Market: Index drops 5%' }), ...history];
    await act(() => vi.advanceTimersByTimeAsync(REFRESH_INTERVAL_MS));

    expect(await screen.findByText('[HIGH] Market: Index drops 5%')).toBeInTheDocument();
    expect(backend.callsTo('GET /api/users/1/notifications')).toHaveLength(2);
  });

  it('refreshes on demand and follows the user switch', async () => {
    const backend = fakeApi({
      'GET /api/users': [ALICE, BOB],
      'GET /api/users/1/notifications': [delivery()],
      'GET /api/users/2/notifications': [],
    });
    const user = userEvent.setup();
    renderApp('/notifications');
    await screen.findByText('Sent');

    await user.click(screen.getByRole('button', { name: 'Refresh' }));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Acting as' }), 'Bob Trader');

    expect(await screen.findByText(/No notifications yet/)).toBeInTheDocument();
    expect(backend.callsTo('GET /api/users/1/notifications')).toHaveLength(2);
  });
});
