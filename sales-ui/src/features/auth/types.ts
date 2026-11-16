export type UserRole = 'ADMIN' | 'STAFF';

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
  timezone: string;
}
