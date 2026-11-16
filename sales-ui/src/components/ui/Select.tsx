import { useId, type SelectHTMLAttributes } from 'react';
import { ChevronDown } from 'lucide-react';

interface SelectOption {
  value: string | number;
  label: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  placeholder?: string;
  options: SelectOption[];
  error?: string;
}

export function Select({
  label,
  placeholder,
  options,
  error,
  className = '',
  id,
  ...otherProps
}: SelectProps) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
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
        <select
          id={fieldId}
          className={`w-full appearance-none pl-3.5 pr-10 py-2.5 rounded-xl bg-white border text-sm text-ink shadow-2xs focus:outline-none focus:ring-2 transition ${borderStyle} ${className}`}
          {...otherProps}
        >
          {placeholder && <option value="">{placeholder}</option>}
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <ChevronDown className="w-4 h-4 text-muted absolute right-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
      </div>
      {error && <p className="text-xs font-medium text-rose-600">{error}</p>}
    </div>
  );
}
