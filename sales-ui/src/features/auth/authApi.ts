import { API_BASE_URL } from '../../constants';
import { jsonRequest, readJson } from '../../api/httpClient';
import type { AuthResponse } from './types';

interface Credentials {
  username: string;
  password: string;
}

export const authApi = {
  login: (credentials: Credentials) =>
    fetch(`${API_BASE_URL}/auth/login`, jsonRequest('POST', credentials)).then((response) =>
      readJson<AuthResponse>(response)
    ),
  register: (credentials: Credentials) =>
    fetch(`${API_BASE_URL}/auth/register`, jsonRequest('POST', credentials)).then((response) =>
      readJson<AuthResponse>(response)
    ),
};
