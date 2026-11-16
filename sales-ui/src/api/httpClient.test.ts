import { afterEach, describe, expect, it, vi } from 'vitest';
import { AUTH_STORAGE } from '../constants';
import { fetchWithAuth, readJson, throwProblem } from './httpClient';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
});

describe('readJson', () => {
  it('unwraps the data field of the response envelope', async () => {
    const customer = await readJson<{ customerId: number }>(jsonResponse({ message: 'OK', data: { customerId: 7 } }));

    expect(customer).toEqual({ customerId: 7 });
  });

  it('uses the problem detail when the request failed', async () => {
    const failed = jsonResponse({ title: 'Business Rule Violation', detail: 'Office email is already registered.' }, 400);

    await expect(readJson(failed)).rejects.toThrow('Office email is already registered.');
  });

  it('falls back to the problem title, then to the status code', async () => {
    await expect(readJson(jsonResponse({ title: 'Access Denied' }, 403))).rejects.toThrow('Access Denied');
    await expect(readJson(jsonResponse({}, 502))).rejects.toThrow('Request failed (502)');
  });
});

describe('throwProblem', () => {
  it('uses the fallback message when the body is not JSON', async () => {
    const htmlError = new Response('<html>gateway timeout</html>', { status: 504 });

    await expect(throwProblem(htmlError, 'Failed to generate PDF report')).rejects.toThrow('Failed to generate PDF report');
  });
});

describe('fetchWithAuth', () => {
  it('sends the stored token as a bearer header', async () => {
    localStorage.setItem(AUTH_STORAGE.TOKEN_KEY, 'jwt-123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ data: {} }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchWithAuth('/sales');

    const sentHeaders = fetchMock.mock.calls[0][1].headers as Headers;
    expect(sentHeaders.get('Authorization')).toBe('Bearer jwt-123');
  });

  it('clears the session and announces it when the API answers 401', async () => {
    localStorage.setItem(AUTH_STORAGE.TOKEN_KEY, 'expired');
    localStorage.setItem(AUTH_STORAGE.ROLE_KEY, 'STAFF');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ title: 'Authentication Failed' }, 401)));
    const unauthorized = vi.fn();
    window.addEventListener(AUTH_STORAGE.UNAUTHORIZED_EVENT, unauthorized);

    await fetchWithAuth('/sales');

    expect(localStorage.getItem(AUTH_STORAGE.TOKEN_KEY)).toBeNull();
    expect(unauthorized).toHaveBeenCalledOnce();
    window.removeEventListener(AUTH_STORAGE.UNAUTHORIZED_EVENT, unauthorized);
  });
});
