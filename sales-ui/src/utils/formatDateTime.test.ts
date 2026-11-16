import { afterEach, describe, expect, it, vi } from 'vitest';
import { AUTH_STORAGE } from '../constants';
import { getBusinessTimezone } from './businessTimezone';
import { formatDateTime } from './formatDateTime';

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('formatDateTime', () => {
  it('shows an early-morning sale on the Malaysian calendar day', () => {
    const formatted = formatDateTime('2026-11-15T16:30:00Z', 'Asia/Kuala_Lumpur');

    expect(formatted).toContain('16/11/2026');
    expect(formatted).toContain('00:30');
  });

  it('shows the same instant on the previous day in UTC', () => {
    const formatted = formatDateTime('2026-11-15T16:30:00Z', 'UTC');

    expect(formatted).toContain('15/11/2026');
    expect(formatted).toContain('16:30');
  });

  it('uses Malaysia time when nothing is stored yet', () => {
    const formatted = formatDateTime('2026-11-15T16:30:00Z');

    expect(formatted).toContain('16/11/2026');
    expect(formatted).toContain('00:30');
  });
});

describe('getBusinessTimezone', () => {
  it('returns the timezone saved at login', () => {
    localStorage.setItem(AUTH_STORAGE.TIMEZONE_KEY, 'UTC');

    expect(getBusinessTimezone()).toBe('UTC');
  });

  it('falls back to Malaysia time when storage is empty', () => {
    expect(getBusinessTimezone()).toBe('Asia/Kuala_Lumpur');
  });

  it('falls back to Malaysia time when storage is blocked', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });

    expect(getBusinessTimezone()).toBe('Asia/Kuala_Lumpur');
  });
});
