import type { FormEvent, FormHTMLAttributes, ReactNode } from 'react';

interface FormProps extends Omit<FormHTMLAttributes<HTMLFormElement>, 'onSubmit'> {
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  children: ReactNode;
}

export function Form({ onSubmit, children, className = '', ...otherProps }: FormProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit(event);
  };

  return (
    <form onSubmit={handleSubmit} className={`space-y-4 ${className}`} {...otherProps}>
      {children}
    </form>
  );
}
