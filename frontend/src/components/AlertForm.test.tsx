import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { rule } from '../test/fakeApi';
import { AlertForm } from './AlertForm';

function setup(props: Partial<Parameters<typeof AlertForm>[0]> = {}) {
  const onSubmit = vi.fn().mockResolvedValue(undefined);
  const onCancel = vi.fn();
  render(<AlertForm defaultEmail="alice@example.com" onSubmit={onSubmit} onCancel={onCancel} {...props} />);
  return { user: userEvent.setup(), onSubmit, onCancel };
}

describe('AlertForm', () => {
  it('starts a new alert with sensible defaults', () => {
    setup();

    expect(screen.getByRole('heading', { name: 'New alert' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Disaster' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'News' })).not.toBeChecked();
    expect(screen.getByRole('combobox', { name: 'Minimum severity' })).toHaveValue('HIGH');
    expect(screen.getByRole('textbox', { name: 'Address 1' })).toHaveValue('alice@example.com');
    expect(screen.getByRole('checkbox', { name: 'Enabled' })).toBeChecked();
  });

  it('submits exactly what the backend expects', async () => {
    const { user, onSubmit } = setup();

    await user.type(screen.getByRole('textbox', { name: 'Name' }), '  Tokyo quakes ');
    await user.click(screen.getByRole('checkbox', { name: 'News' }));
    await user.type(screen.getByRole('textbox', { name: /Keywords/ }), 'earthquake, Tokyo, ');
    await user.selectOptions(screen.getByRole('combobox', { name: 'Minimum severity' }), 'CRITICAL');
    await user.click(screen.getByRole('button', { name: 'Add target' }));
    await user.type(screen.getByRole('textbox', { name: 'Address 2' }), ' http://localhost:8089/services/demo ');
    await user.click(screen.getByRole('button', { name: 'Create alert' }));

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Tokyo quakes',
      categories: ['NEWS', 'DISASTER'], // checkbox order, not click order
      keywords: ['earthquake', 'Tokyo'],
      minSeverity: 'CRITICAL',
      targets: [
        { type: 'EMAIL', address: 'alice@example.com' },
        { type: 'SLACK', address: 'http://localhost:8089/services/demo' },
      ],
      enabled: true,
    });
  });

  it.each([
    ['a name', async () => {}, 'Give the alert a name.'],
    [
      'a category',
      async (user: ReturnType<typeof userEvent.setup>) =>
        user.click(screen.getByRole('checkbox', { name: 'Disaster' })),
      'Choose at least one category.',
    ],
    [
      'a target address',
      async (user: ReturnType<typeof userEvent.setup>) => user.clear(screen.getByRole('textbox', { name: 'Address 1' })),
      'Every notification target needs an address.',
    ],
  ])('does not submit without %s', async (_, breakIt, message) => {
    const { user, onSubmit } = setup();
    if (message !== 'Give the alert a name.') {
      await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Quakes');
    }
    await breakIt(user);

    await user.click(screen.getByRole('button', { name: 'Create alert' }));

    expect(screen.getByRole('alert')).toHaveTextContent(message);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows the backend error and its field errors, and lets the user try again', async () => {
    const { user, onSubmit } = setup();
    onSubmit.mockRejectedValueOnce(
      new ApiError(400, 'SLACK target must be an http(s) webhook URL: nope', ['targets[0].address: invalid']),
    );
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Quakes');

    await user.click(screen.getByRole('button', { name: 'Create alert' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('SLACK target must be an http(s) webhook URL: nope');
    expect(alert).toHaveTextContent('targets[0].address: invalid');
    expect(screen.getByRole('button', { name: 'Create alert' })).toBeEnabled();
  });

  it('can create an alert that starts switched off', async () => {
    const { user, onSubmit } = setup();
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Later');

    await user.click(screen.getByRole('checkbox', { name: 'Enabled' }));
    await user.click(screen.getByRole('button', { name: 'Create alert' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ enabled: false }));
  });

  it('shows an unexpected error that is not from the backend', async () => {
    const { user, onSubmit } = setup();
    onSubmit.mockRejectedValueOnce(new TypeError('Something broke'));
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Quakes');

    await user.click(screen.getByRole('button', { name: 'Create alert' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Something broke');
    expect(within(alert).queryByRole('list')).not.toBeInTheDocument();
  });

  it('edits an existing rule with its current values', async () => {
    const existing = rule({
      name: 'Markets',
      categories: ['MARKET'],
      keywords: ['crash', 'bitcoin'],
      minSeverity: 'MEDIUM',
      targets: [{ type: 'SLACK', address: 'http://localhost:8089/services/x' }],
      enabled: false,
    });
    const { user, onSubmit } = setup({ rule: existing });

    expect(screen.getByRole('heading', { name: 'Edit “Markets”' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /Keywords/ })).toHaveValue('crash, bitcoin');
    expect(screen.getByRole('checkbox', { name: 'Enabled' })).not.toBeChecked();

    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Markets',
      categories: ['MARKET'],
      keywords: ['crash', 'bitcoin'],
      minSeverity: 'MEDIUM',
      targets: [{ type: 'SLACK', address: 'http://localhost:8089/services/x' }],
      enabled: false,
    });
  });

  it('keeps between one and ten targets', async () => {
    const { user } = setup();
    const remove = () => screen.getAllByRole('button', { name: /Remove target/ });

    expect(remove()[0]).toBeDisabled();
    for (let i = 0; i < 9; i++) {
      await user.click(screen.getByRole('button', { name: 'Add target' }));
    }
    expect(remove()).toHaveLength(10);
    expect(screen.getByRole('button', { name: 'Add target' })).toBeDisabled();

    await user.click(remove()[0]);
    expect(remove()).toHaveLength(9);
    expect(screen.getByRole('textbox', { name: 'Address 1' })).toHaveValue('');
  });

  it('switches the placeholder with the channel and can be cancelled', async () => {
    const { user, onCancel } = setup();

    await user.selectOptions(screen.getByRole('combobox', { name: 'Channel 1' }), 'SLACK');
    expect(screen.getByRole('textbox', { name: 'Address 1' })).toHaveAttribute(
      'placeholder',
      expect.stringContaining('hooks.slack.com'),
    );

    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalled();
  });
});
