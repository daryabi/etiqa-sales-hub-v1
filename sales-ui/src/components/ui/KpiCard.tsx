import type { ReactNode } from 'react';

interface KpiCardProps {
  label: string;
  value: string | number;
  caption: string;
  icon: ReactNode;
}

export function KpiCard({ label, value, caption, icon }: KpiCardProps) {
  return (
    <div className="p-5 rounded-2xl bg-white border border-gray-200 border-t-4 border-t-brand shadow-xs">
      <div className="flex items-center justify-between text-muted text-xs font-bold tracking-wider">
        <span>{label}</span>
        <span className="p-2 rounded-xl bg-brand-tint text-ink">{icon}</span>
      </div>
      <p className="text-2xl font-extrabold text-ink mt-2">{value}</p>
      <p className="text-xs text-muted mt-1">{caption}</p>
    </div>
  );
}
