import { act, cleanup, render, renderHook, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { APP_CONFIG } from '../constants';
import { NotificationProvider } from './NotificationProvider';
import { useNotify } from './notificationContext';

function SaveButton() {
  const notify = useNotify();
  return <button onClick={() => notify('success', 'Customer saved.')}>Save</button>;
}

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

describe('NotificationProvider', () => {
  it('shows the message and hides it after the toast duration', () => {
    vi.useFakeTimers();
    render(
      <NotificationProvider>
        <SaveButton />
      </NotificationProvider>
    );

    act(() => screen.getByRole('button', { name: 'Save' }).click());
    expect(screen.getByText('Customer saved.')).toBeTruthy();

    act(() => vi.advanceTimersByTime(APP_CONFIG.TOAST_DURATION_MS));
    expect(screen.queryByText('Customer saved.')).toBeNull();
  });

  it('useNotify outside the provider fails loudly', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => renderHook(() => useNotify())).toThrow('useNotify must be used within a NotificationProvider');
  });
});
