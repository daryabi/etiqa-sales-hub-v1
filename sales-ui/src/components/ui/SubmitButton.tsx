import type { ReactNode } from 'react';
import { Button } from './Button';

interface SubmitButtonProps {
  isPending: boolean;
  loadingText?: string;
  icon?: ReactNode;
  children: ReactNode;
}

export function SubmitButton({ isPending, loadingText = 'Saving...', icon, children }: SubmitButtonProps) {
  return (
    <Button type="submit" variant="primary" fullWidth isLoading={isPending} loadingText={loadingText} icon={icon}>
      {children}
    </Button>
  );
}
