import { renderHook, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ALICE, BOB, fakeApi } from './test/fakeApi';
import { renderApp } from './test/renderApp';
import { useUsers } from './UserContext';

describe('UserContext', () => {
  // Private browsing or strict privacy settings can make localStorage throw.
  it('still starts with the first user and still switches, it just does not remember', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    fakeApi({ 'GET /api/users': [ALICE, BOB], 'GET /api/users/1/alerts': [], 'GET /api/users/2/alerts': [] });
    const user = userEvent.setup();
    renderApp();
    const switcher = await screen.findByRole('combobox', { name: 'Acting as' });
    expect(switcher).toHaveValue('1');

    await user.selectOptions(switcher, 'Bob Trader');

    expect(switcher).toHaveValue('2');
  });

  it('requires the provider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => renderHook(() => useUsers())).toThrow('useUsers must be used inside <UserProvider>');
  });
});
