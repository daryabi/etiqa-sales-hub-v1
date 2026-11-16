import type { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'success' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: ReactNode;
  isLoading?: boolean;
  loadingText?: string;
  fullWidth?: boolean;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    'bg-brand hover:bg-brand-hover text-ink font-bold shadow-sm border border-brand',
  secondary:
    'bg-ink hover:bg-neutral-800 text-white font-semibold shadow-sm border border-ink',
  outline:
    'bg-white hover:bg-brand-tint text-ink font-semibold border border-gray-300 hover:border-brand',
  success:
    'bg-emerald-600 hover:bg-emerald-500 text-white font-semibold shadow-sm border border-emerald-600',
  ghost:
    'bg-transparent hover:bg-gray-100 text-muted hover:text-ink font-semibold border border-transparent',
};

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-xs rounded-lg gap-1.5',
  md: 'px-4 py-2.5 text-sm rounded-xl gap-2',
  lg: 'px-5 py-3 text-sm rounded-xl gap-2',
};

export function Button({
  variant = 'primary',
  size = 'md',
  icon,
  isLoading = false,
  loadingText,
  fullWidth = false,
  disabled,
  className = '',
  children,
  ...otherProps
}: ButtonProps) {
  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <button
      disabled={disabled || isLoading}
      className={`inline-flex items-center justify-center transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${VARIANT_CLASSES[variant]} ${SIZE_CLASSES[size]} ${widthClass} ${className}`}
      {...otherProps}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      <span>{isLoading && loadingText ? loadingText : children}</span>
    </button>
  );
}
