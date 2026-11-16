import { useId, type InputHTMLAttributes, type ReactNode } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  leftIcon?: ReactNode;
  error?: string;
}

export function Input({
  label,
  leftIcon,
  error,
  className = '',
  id,
  ...otherProps
}: InputProps) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
  const paddingLeft = leftIcon ? 'pl-10' : 'pl-3.5';
  const borderStyle = error
    ? 'border-rose-500 focus:ring-rose-500/30 focus:border-rose-500'
    : 'border-gray-200 focus:ring-brand focus:border-ink';

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label htmlFor={fieldId} className="block text-xs font-semibold text-ink">
          {label}
        </label>
      )}
      <div className="relative">
        {leftIcon && (
          <span className="text-muted absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none">
            {leftIcon}
          </span>
        )}
        <input
          id={fieldId}
          className={`w-full ${paddingLeft} pr-3.5 py-2.5 rounded-xl bg-white border text-sm text-ink placeholder-gray-400 shadow-2xs focus:outline-none focus:ring-2 transition ${borderStyle} ${className}`}
          {...otherProps}
        />
      </div>
      {error && <p className="text-xs font-medium text-rose-600">{error}</p>}
    </div>
  );
}
