import type { ReactNode } from 'react';

interface CardProps {
  title?: string;
  icon?: ReactNode;
  subtitle?: string;
  headerAction?: ReactNode;
  children: ReactNode;
  className?: string;
}

export function Card({
  title,
  icon,
  subtitle,
  headerAction,
  children,
  className = '',
}: CardProps) {
  return (
    <div className={`p-6 rounded-2xl bg-white border border-gray-200 shadow-xs space-y-4 ${className}`}>
      {(title || headerAction) && (
        <div className="flex items-start justify-between gap-3 border-b border-gray-100 pb-3">
          <div>
            {title && (
              <h2 className="text-base font-bold text-ink flex items-center gap-2">
                {icon && <span className="p-1.5 rounded-lg bg-brand-tint text-ink">{icon}</span>}
                <span>{title}</span>
              </h2>
            )}
            {subtitle && <p className="text-xs text-muted mt-1">{subtitle}</p>}
          </div>
          {headerAction && <div>{headerAction}</div>}
        </div>
      )}
      {children}
    </div>
  );
}
