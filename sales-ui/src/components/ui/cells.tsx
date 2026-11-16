import type { ReactNode } from 'react';
import { formatDateTime } from '../../utils/formatDateTime';
import { formatMoney } from '../../utils/formatMoney';

export function IdCell({ id }: { id: number | null }) {
  if (id === null) {
    return <>—</>;
  }
  return <span className="font-mono font-bold text-ink">#{id}</span>;
}

export function PrimaryText({ children }: { children: ReactNode }) {
  return <span className="font-semibold text-ink">{children}</span>;
}

export function MoneyCell({ amount, highlight = false }: { amount: number | string; highlight?: boolean }) {
  if (highlight) {
    return <span className="font-bold text-emerald-700">{formatMoney(amount)}</span>;
  }
  return <>{formatMoney(amount)}</>;
}

export function DateTimeCell({ value }: { value: string }) {
  return <span className="text-xs text-muted whitespace-nowrap">{formatDateTime(value)}</span>;
}
