import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ChannelStatus, WorldEvent } from '../../api/types';
import { ALICE, BOB, delivery, fakeApi, rule } from '../../test/fakeApi';
import { renderApp } from '../../test/renderApp';
import { REFRESH_INTERVAL_MS } from '../../useAutoRefresh';

const CAROL = { id: 3, name: 'Carol Responder', email: 'carol@example.com' };

function event(overrides: Partial<WorldEvent> = {}): WorldEvent {
  return {
    id: 7,
    source: 'SIMULATOR',
    externalId: 'abc',
    category: 'MARKET',
    severity: 'HIGH',
    title: 'Index drops 5%',
    description: null,
    url: null,
    occurredAt: '2026-11-20T15:00:00Z',
    receivedAt: '2026-11-20T15:00:01Z',
    ...overrides,
  };
}

describe('Admin', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('is reachable from the top bar and hides the user switcher', async () => {
    fakeApi({ 'GET /api/users': [ALICE], 'GET /api/users/1/alerts': [], 'GET /api/admin/alerts': [] });
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole('combobox', { name: 'Acting as' });

    await user.click(screen.getByRole('link', { name: 'Admin' }));

    expect(await screen.findByRole('heading', { name: 'Admin' })).toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Acting as' })).not.toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Admin' })).toBeInTheDocument();
  });

  describe('Users and alerts', () => {
    it("groups every user's alerts under the user, including users without alerts", async () => {
      fakeApi({
        'GET /api/users': [ALICE, BOB, CAROL],
        'GET /api/admin/alerts': [
          rule(),
          rule({ id: 11, name: 'Old rule', enabled: false }),
          rule({
            id: 20,
            userId: 2,
            name: 'Markets',
            categories: ['NEWS', 'MARKET'],
            keywords: [],
            minSeverity: 'MEDIUM',
            targets: [{ type: 'SLACK', address: 'http://localhost:8089/services/bob' }],
          }),
        ],
      });

      renderApp('/admin');

      const alice = await screen.findByRole('article', { name: 'Alice Analyst' });
      expect(within(alice).getByText(/2 alerts/)).toBeInTheDocument();
      expect(within(alice).getAllByRole('row')).toHaveLength(3); // header + 2
      expect(within(alice).getByRole('row', { name: /Old rule/ })).toHaveTextContent('Off');
      const bob = screen.getByRole('article', { name: 'Bob Trader' });
      expect(within(bob).getByText(/1 alert$/)).toBeInTheDocument();
      expect(within(bob).getByRole('row', { name: /Markets/ })).toHaveTextContent(
        /News, Market.*Medium\+.*any.*Slack http:\/\/localhost:8089\/services\/bob.*On/,
      );
      const carol = screen.getByRole('article', { name: 'Carol Responder' });
      expect(within(carol).getByText('No alerts.')).toBeInTheDocument();
    });
  });

  describe('Events', () => {
    it('lists recent events with source, category, severity and a link when there is one', async () => {
      fakeApi({
        'GET /api/users': [ALICE],
        'GET /api/events': [
          event(),
          event({ id: 8, source: 'USGS', category: 'DISASTER', severity: 'CRITICAL', title: 'M 7.1 - Japan', url: 'https://earthquake.usgs.gov/x' }),
        ],
      });

      renderApp('/admin/events');

      const rows = await screen.findAllByRole('row');
      expect(rows[1]).toHaveTextContent(/20 Nov 2026, 15:00.*SIMULATOR.*Market.*High.*Index drops 5%/);
      expect(within(rows[2]).getByRole('link', { name: 'M 7.1 - Japan' })).toHaveAttribute(
        'href',
        'https://earthquake.usgs.gov/x',
      );
    });

    it('publishes a test event, reports how many alerts it matched and refreshes the list', async () => {
      let events = [event()];
      const backend = fakeApi({
        'GET /api/users': [ALICE],
        'GET /api/events': () => events,
        'POST /api/admin/events': ({ body }) => {
          const published = event({ id: 9, source: 'ADMIN', externalId: null, ...(body as object) });
          events = [published, ...events];
          return { status: 201, body: { event: published, duplicate: false, matchedAlertRuleIds: [10, 20] } };
        },
      });
      const user = userEvent.setup();
      renderApp('/admin/events');
      await screen.findByText('Index drops 5%');

      await user.selectOptions(screen.getByRole('combobox', { name: 'Category' }), 'DISASTER');
      await user.selectOptions(screen.getByRole('combobox', { name: 'Severity' }), 'HIGH');
      await user.type(screen.getByRole('textbox', { name: 'Title' }), '  M 7.2 earthquake near Tokyo ');
      await user.click(screen.getByRole('button', { name: 'Publish event' }));

      expect(await screen.findByRole('status')).toHaveTextContent(
        'Published event 9. It matched 2 alerts; see the delivery log for the notifications.',
      );
      expect(backend.callsTo('POST /api/admin/events')[0].body).toEqual({
        category: 'DISASTER',
        severity: 'HIGH',
        title: 'M 7.2 earthquake near Tokyo',
      });
      expect(await screen.findByText('M 7.2 earthquake near Tokyo')).toBeInTheDocument();
      expect(screen.getByRole('textbox', { name: 'Title' })).toHaveValue('');
    });

    it('says plainly when nobody was notified, sends the description only when given, and replaces the message', async () => {
      const matches = [[], [10]];
      const backend = fakeApi({
        'GET /api/users': [ALICE],
        'GET /api/events': [],
        'POST /api/admin/events': () => {
          const id = 9 + backend.callsTo('POST /api/admin/events').length;
          return {
            status: 201,
            body: { event: event({ id }), duplicate: false, matchedAlertRuleIds: matches.shift() },
          };
        },
      });
      const user = userEvent.setup();
      renderApp('/admin/events');
      await screen.findByText('No events yet.');

      await user.type(screen.getByRole('textbox', { name: 'Title' }), 'Quiet day');
      await user.type(screen.getByRole('textbox', { name: /Description/ }), 'Nothing happened.');
      await user.click(screen.getByRole('button', { name: 'Publish event' }));

      expect(await screen.findByRole('status')).toHaveTextContent('It matched no alerts, so nobody was notified.');
      expect(backend.callsTo('POST /api/admin/events')[0].body).toMatchObject({ description: 'Nothing happened.' });

      await user.type(screen.getByRole('textbox', { name: 'Title' }), 'Earthquake');
      await user.click(screen.getByRole('button', { name: 'Publish event' }));
      expect(await screen.findByText(/It matched 1 alert;/)).toBeInTheDocument();
      expect(screen.getAllByRole('status')).toHaveLength(1);
      expect(backend.callsTo('POST /api/admin/events')[1].body).not.toHaveProperty('description');
    });

    it('needs a title and shows backend errors', async () => {
      const backend = fakeApi({
        'GET /api/users': [ALICE],
        'GET /api/events': [],
        'POST /api/admin/events': {
          status: 400,
          body: { detail: 'Request validation failed', errors: ['title: size must be between 0 and 300'] },
        },
      });
      const user = userEvent.setup();
      renderApp('/admin/events');
      await screen.findByText('No events yet.');

      await user.click(screen.getByRole('button', { name: 'Publish event' }));
      expect(screen.getByRole('alert')).toHaveTextContent('Give the event a title.');
      expect(backend.callsTo('POST /api/admin/events')).toHaveLength(0);

      await user.type(screen.getByRole('textbox', { name: 'Title' }), 'x');
      await user.click(screen.getByRole('button', { name: 'Publish event' }));
      expect(await screen.findByRole('alert')).toHaveTextContent('title: size must be between 0 and 300');
    });

    it('refreshes the list by itself', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      let events = [event()];
      fakeApi({ 'GET /api/users': [ALICE], 'GET /api/events': () => events });
      renderApp('/admin/events');
      await screen.findByText('Index drops 5%');

      events = [event({ id: 12, title: 'Rail strike announced', category: 'NEWS' }), ...events];
      await act(() => vi.advanceTimersByTimeAsync(REFRESH_INTERVAL_MS));

      expect(await screen.findByText('Rail strike announced')).toBeInTheDocument();
    });
  });

  describe('Delivery log', () => {
    it("shows every user's deliveries with the user's name and filters by status", async () => {
      const backend = fakeApi({
        'GET /api/users': [ALICE, BOB],
        'GET /api/admin/deliveries': ({ url }) =>
          url.includes('status=FAILED')
            ? [delivery({ id: 2, userId: 2, status: 'FAILED', attempts: 3, lastError: 'Slack webhook call failed: 500' })]
            : [delivery({ id: 1 }), delivery({ id: 2, userId: 2, status: 'FAILED', attempts: 3 }), delivery({ id: 3, userId: 9 })],
      });
      const user = userEvent.setup();
      renderApp('/admin/deliveries');

      const rows = await screen.findAllByRole('row');
      expect(rows).toHaveLength(4);
      expect(rows[1]).toHaveTextContent('Alice Analyst');
      expect(rows[2]).toHaveTextContent(/Failed after 3 attempts.*Bob Trader/);
      expect(rows[3]).toHaveTextContent('User 9'); // a user the admin view does not know (yet)
      expect(backend.callsTo('GET /api/admin/deliveries')[0].url).toBe('/api/admin/deliveries?limit=100');

      await user.selectOptions(screen.getByRole('combobox', { name: 'Status' }), 'FAILED');

      await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(2));
      expect(backend.callsTo('GET /api/admin/deliveries').at(-1)?.url).toBe(
        '/api/admin/deliveries?limit=100&status=FAILED',
      );

      await user.click(screen.getAllByRole('row')[1]);
      expect(screen.getByText('Slack webhook call failed: 500')).toBeInTheDocument();
      expect(screen.getByRole('cell', { name: /Category: Disaster/ })).toHaveAttribute('colspan', '6');
    });

    it('explains an empty result for a filter', async () => {
      fakeApi({ 'GET /api/users': [ALICE], 'GET /api/admin/deliveries': [] });
      const user = userEvent.setup();
      renderApp('/admin/deliveries');
      expect(await screen.findByText('No deliveries.')).toBeInTheDocument();

      await user.selectOptions(screen.getByRole('combobox', { name: 'Status' }), 'SKIPPED');

      expect(await screen.findByText('No deliveries with this status.')).toBeInTheDocument();

      await user.selectOptions(screen.getByRole('combobox', { name: 'Status' }), '');
      expect(await screen.findByText('No deliveries.')).toBeInTheDocument();
    });
  });

  describe('Channels', () => {
    it('switches a channel off and on again', async () => {
      let channels: ChannelStatus[] = [
        { type: 'EMAIL', enabled: true, implemented: true },
        { type: 'SLACK', enabled: true, implemented: true },
      ];
      const backend = fakeApi({
        'GET /api/users': [ALICE],
        'GET /api/admin/channels': () => channels,
        'PUT /api/admin/channels/SLACK': ({ body }) => {
          channels = channels.map((c) => (c.type === 'SLACK' ? { ...c, ...(body as object) } : c));
          return channels[1];
        },
      });
      const user = userEvent.setup();
      renderApp('/admin/channels');
      const slack = await screen.findByRole('article', { name: 'Slack' });

      await user.click(within(slack).getByRole('button', { name: 'Switch Slack off' }));
      expect(await within(slack).findByText('Off')).toBeInTheDocument();
      await user.click(within(slack).getByRole('button', { name: 'Switch Slack on' }));
      expect(await within(slack).findByText('On')).toBeInTheDocument();

      expect(backend.callsTo('PUT /api/admin/channels/SLACK').map((c) => c.body)).toEqual([
        { enabled: false },
        { enabled: true },
      ]);
      expect(within(screen.getByRole('article', { name: 'Email' })).getByText('On')).toBeInTheDocument();
    });

    it('warns about a channel without implementation and shows a failed switch', async () => {
      fakeApi({
        'GET /api/users': [ALICE],
        'GET /api/admin/channels': [{ type: 'EMAIL', enabled: true, implemented: false }],
        'PUT /api/admin/channels/EMAIL': { status: 500, body: { detail: 'Database unavailable' } },
      });
      const user = userEvent.setup();
      renderApp('/admin/channels');
      const email = await screen.findByRole('article', { name: 'Email' });
      expect(within(email).getByText('No implementation is deployed for this channel.')).toBeInTheDocument();

      await user.click(within(email).getByRole('button', { name: 'Switch Email off' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('Database unavailable');
      expect(within(email).getByText('On')).toBeInTheDocument();
    });
  });
});
