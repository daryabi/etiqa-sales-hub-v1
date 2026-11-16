import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { AlertBanner } from '../components/ui';
import { APP_CONFIG } from '../constants';
import { NotificationContext, type NotificationType } from './notificationContext';

interface Notification {
  type: NotificationType;
  text: string;
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notification, setNotification] = useState<Notification | null>(null);

  const notify = useCallback((type: NotificationType, text: string) => {
    setNotification({ type, text });
  }, []);

  useEffect(() => {
    if (!notification) {
      return;
    }
    const timer = window.setTimeout(() => setNotification(null), APP_CONFIG.TOAST_DURATION_MS);
    return () => window.clearTimeout(timer);
  }, [notification]);

  return (
    <NotificationContext.Provider value={notify}>
      {notification && <AlertBanner type={notification.type} message={notification.text} />}
      {children}
    </NotificationContext.Provider>
  );
}
