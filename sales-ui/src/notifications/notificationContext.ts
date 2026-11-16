import { createContext, useContext } from 'react';

export type NotificationType = 'success' | 'error';
export type Notify = (type: NotificationType, text: string) => void;

export const NotificationContext = createContext<Notify | undefined>(undefined);

export function useNotify(): Notify {
  const notify = useContext(NotificationContext);
  if (!notify) {
    throw new Error('useNotify must be used within a NotificationProvider');
  }
  return notify;
}
