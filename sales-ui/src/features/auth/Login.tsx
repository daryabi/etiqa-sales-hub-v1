import React, { useState } from 'react';
import { AlertCircle, BookOpen, Lock, User } from 'lucide-react';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  USERNAME_MAX_LENGTH,
  USERNAME_MIN_LENGTH,
  USERNAME_PATTERN,
} from './constants';
import { authApi } from './authApi';
import { useAuth } from './authContextStore';

type AuthMode = 'signIn' | 'signUp';

const INPUT_CLASSES =
  'w-full pl-10 pr-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand focus:border-brand';

function signUpProblem(username: string, password: string, confirmPassword: string): string | null {
  if (!USERNAME_PATTERN.test(username)) {
    return 'Username may only contain letters, digits, dots, underscores, and hyphens.';
  }
  if (password !== confirmPassword) {
    return 'Passwords do not match.';
  }
  return null;
}

export const Login: React.FC = () => {
  const { login } = useAuth();
  const [mode, setMode] = useState<AuthMode>('signIn');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const isSignUp = mode === 'signUp';

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setError('');
    setPassword('');
    setConfirmPassword('');
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');

    if (isSignUp) {
      const problem = signUpProblem(username.trim(), password, confirmPassword);
      if (problem) {
        setError(problem);
        return;
      }
    }

    setLoading(true);
    try {
      const credentials = { username: username.trim(), password };
      const response = isSignUp ? await authApi.register(credentials) : await authApi.login(credentials);
      login(response);
    } catch (requestError) {
      setError(isSignUp ? (requestError as Error).message : 'Invalid username or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-canvas flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="mx-auto w-14 h-14 rounded-2xl bg-ink border-2 border-brand flex items-center justify-center text-brand shadow-md">
          <BookOpen className="w-7 h-7" />
        </div>
        <h2 className="mt-5 text-2xl font-extrabold text-ink">
          {isSignUp ? 'Create your Etiqa Sales Hub account' : 'Sign in to Etiqa Sales Hub'}
        </h2>
        <p className="mt-1.5 text-sm text-gray-500">
          Manage POS transactions, inventory, reports, and audit logs
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm rounded-2xl border border-gray-200 border-t-4 border-t-brand">
          <div className="grid grid-cols-2 gap-1 p-1 mb-6 rounded-xl bg-canvas border border-gray-200" role="tablist">
            <ModeTab active={!isSignUp} label="Sign In" onClick={() => switchMode('signIn')} />
            <ModeTab active={isSignUp} label="Sign Up" onClick={() => switchMode('signUp')} />
          </div>

          <form className="space-y-5" onSubmit={handleSubmit}>
            <Field label="Username" icon={<User className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />}>
              <input
                type="text"
                required
                autoComplete="username"
                minLength={isSignUp ? USERNAME_MIN_LENGTH : undefined}
                maxLength={USERNAME_MAX_LENGTH}
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                className={INPUT_CLASSES}
              />
            </Field>

            <Field label="Password" icon={<Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />}>
              <input
                type="password"
                required
                autoComplete={isSignUp ? 'new-password' : 'current-password'}
                minLength={isSignUp ? PASSWORD_MIN_LENGTH : undefined}
                maxLength={PASSWORD_MAX_LENGTH}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className={INPUT_CLASSES}
              />
            </Field>

            {isSignUp && (
              <Field label="Confirm Password" icon={<Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />}>
                <input
                  type="password"
                  required
                  autoComplete="new-password"
                  maxLength={PASSWORD_MAX_LENGTH}
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  className={INPUT_CLASSES}
                />
              </Field>
            )}

            {error && (
              <div className="rounded-xl bg-red-50 border border-red-200 p-3.5 flex items-center gap-2.5 text-sm text-red-700">
                <AlertCircle className="h-4 w-4 text-red-500 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 px-4 rounded-xl text-sm font-bold text-ink bg-brand hover:bg-brand-hover transition-colors shadow-sm disabled:opacity-60 cursor-pointer"
            >
              {isSignUp
                ? loading ? 'Creating account...' : 'Create Account'
                : loading ? 'Signing in...' : 'Sign In'}
            </button>

            {isSignUp && (
              <p className="text-xs text-gray-500 text-center">
                New accounts get staff access. An administrator can grant more.
              </p>
            )}
          </form>

          {import.meta.env.DEV && !isSignUp && (
            <div className="mt-6 pt-4 border-t border-gray-100 flex items-center justify-between text-xs text-gray-500">
              <span>Local accounts:</span>
              <div className="flex gap-1.5">
                <button
                  type="button"
                  onClick={() => {
                    setUsername('staff');
                    setPassword('staff123');
                  }}
                  className="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 font-medium text-gray-700 cursor-pointer"
                >
                  staff
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setUsername('admin');
                    setPassword('admin123');
                  }}
                  className="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 font-medium text-gray-700 cursor-pointer"
                >
                  admin
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const ModeTab = ({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) => (
  <button
    type="button"
    role="tab"
    aria-selected={active}
    onClick={onClick}
    className={`py-2 rounded-lg text-sm font-bold transition-colors cursor-pointer ${
      active ? 'bg-brand text-ink shadow-xs' : 'text-gray-500 hover:text-ink'
    }`}
  >
    {label}
  </button>
);

const Field = ({ label, icon, children }: { label: string; icon: React.ReactNode; children: React.ReactNode }) => (
  <div>
    <label className="block text-xs font-bold uppercase tracking-wider text-gray-600 mb-1.5">{label}</label>
    <div className="relative">
      {icon}
      {children}
    </div>
  </div>
);
