import type { ReactNode } from 'react';

export type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'brand';

interface BadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
}

const BADGE_CLASSES: Record<BadgeTone, string> = {
  neutral: 'bg-gray-100 text-ink border-gray-200',
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  warning: 'bg-amber-50 text-amber-800 border-amber-300',
  danger: 'bg-rose-50 text-rose-700 border-rose-200',
  brand: 'bg-brand-tint text-ink border-brand',
};

export function Badge({ tone = 'neutral', children }: BadgeProps) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border ${BADGE_CLASSES[tone]}`}>
      {children}
    </span>
  );
}
