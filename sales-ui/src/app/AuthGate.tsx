import React from 'react';
import { useAuth } from '../features/auth/authContextStore';
import { Login } from '../features/auth/Login';
import { AuthenticatedApp } from './AuthenticatedApp';

export const AuthGate: React.FC = () => {
  const { token } = useAuth();
  return token ? <AuthenticatedApp /> : <Login />;
};
