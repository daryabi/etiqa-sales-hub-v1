import React, { useCallback, useState } from 'react';
import { jwtDecode } from 'jwt-decode';
import { AUTH_STORAGE } from '../../constants';
import type { AuthResponse, UserRole } from './types';
import { AuthContext, type AuthUser } from './authContextStore';

interface JwtClaims {
  sub?: string;
  exp?: number;
}

function parseUserFromToken(token: string | null): AuthUser | null {
  if (!token) {
    return null;
  }
  try {
    const tokenClaims = jwtDecode<JwtClaims>(token);
    if (!tokenClaims.sub || (tokenClaims.exp && tokenClaims.exp * 1000 < Date.now())) {
      return null;
    }
    if (!localStorage.getItem(AUTH_STORAGE.TIMEZONE_KEY)) {
      return null;
    }
    const storedRole = localStorage.getItem(AUTH_STORAGE.ROLE_KEY) || 'STAFF';
    const role = (storedRole.replace('ROLE_', '') === 'ADMIN' ? 'ADMIN' : 'STAFF') as UserRole;
    return { username: tokenClaims.sub, role };
  } catch {
    return null;
  }
}

function clearStoredSession() {
  localStorage.removeItem(AUTH_STORAGE.TOKEN_KEY);
  localStorage.removeItem(AUTH_STORAGE.ROLE_KEY);
  localStorage.removeItem(AUTH_STORAGE.TIMEZONE_KEY);
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => {
    const storedToken = localStorage.getItem(AUTH_STORAGE.TOKEN_KEY);
    if (!parseUserFromToken(storedToken)) {
      clearStoredSession();
      return null;
    }
    return storedToken;
  });

  const user = parseUserFromToken(token);

  const logout = useCallback(() => {
    clearStoredSession();
    setToken(null);
  }, []);

  const login = useCallback((session: AuthResponse) => {
    localStorage.setItem(AUTH_STORAGE.TOKEN_KEY, session.token);
    localStorage.setItem(AUTH_STORAGE.ROLE_KEY, session.role.replace('ROLE_', ''));
    localStorage.setItem(AUTH_STORAGE.TIMEZONE_KEY, session.timezone);
    setToken(session.token);
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
