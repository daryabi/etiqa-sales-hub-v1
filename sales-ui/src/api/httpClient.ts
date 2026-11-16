import { API_BASE_URL, AUTH_STORAGE } from '../constants';
import type { PageQuery } from '../types';

export async function fetchWithAuth(path: string, requestOptions: RequestInit = {}): Promise<Response> {
  const token = localStorage.getItem(AUTH_STORAGE.TOKEN_KEY);
  const headers = new Headers(requestOptions.headers);

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...requestOptions, headers });

  if (response.status === 401) {
    localStorage.removeItem(AUTH_STORAGE.TOKEN_KEY);
    localStorage.removeItem(AUTH_STORAGE.ROLE_KEY);
    window.dispatchEvent(new Event(AUTH_STORAGE.UNAUTHORIZED_EVENT));
  }

  return response;
}

export function jsonRequest(method: string, body: unknown): RequestInit & { headers: Record<string, string> } {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

export async function throwProblem(response: Response, fallbackMessage: string): Promise<never> {
  const problem = await response.json().catch(() => ({}));
  throw new Error(problem.detail || problem.title || fallbackMessage);
}

export async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    return throwProblem(response, `Request failed (${response.status})`);
  }
  const envelope: { data: T } = await response.json();
  return envelope.data;
}

export function pageQueryString(query: PageQuery, extraParams: Record<string, string | undefined> = {}): string {
  const searchParams = new URLSearchParams({ page: String(query.page), size: String(query.size) });
  if (query.sort) {
    searchParams.set('sort', query.sort);
  }
  if (query.search) {
    searchParams.set('search', query.search);
  }
  for (const [paramName, paramValue] of Object.entries(extraParams)) {
    if (paramValue) {
      searchParams.set(paramName, paramValue);
    }
  }
  return searchParams.toString();
}

export function getJson<T>(path: string): Promise<T> {
  return fetchWithAuth(path).then((response) => readJson<T>(response));
}

export function postJson<T>(path: string, body: unknown, extraHeaders: Record<string, string> = {}): Promise<T> {
  const request = jsonRequest('POST', body);
  return fetchWithAuth(path, { ...request, headers: { ...request.headers, ...extraHeaders } }).then((response) =>
    readJson<T>(response)
  );
}
