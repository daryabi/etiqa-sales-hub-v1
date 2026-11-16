import React from 'react';
import { AuthGate } from './app/AuthGate';
import { AuthProvider } from './features/auth/AuthContext';

const App: React.FC = () => (
  <AuthProvider>
    <AuthGate />
  </AuthProvider>
);

export default App;
