import { AlertCircle, CheckCircle2 } from 'lucide-react';

interface AlertBannerProps {
  type: 'success' | 'error';
  message: string;
}

export function AlertBanner({ type, message }: AlertBannerProps) {
  const isSuccess = type === 'success';
  const style = isSuccess
    ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
    : 'bg-rose-50 border-rose-300 text-rose-900';

  return (
    <div className={`flex items-center gap-3 px-4 py-3 rounded-2xl border text-sm font-semibold shadow-2xs ${style}`}>
      {isSuccess ? (
        <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
      ) : (
        <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
      )}
      <span>{message}</span>
    </div>
  );
}
