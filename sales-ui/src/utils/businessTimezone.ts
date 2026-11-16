import { APP_CONFIG, AUTH_STORAGE } from '../constants';

export function getBusinessTimezone(): string {
  try {
    return localStorage.getItem(AUTH_STORAGE.TIMEZONE_KEY) || APP_CONFIG.DEFAULT_BUSINESS_TIMEZONE;
  } catch {
    return APP_CONFIG.DEFAULT_BUSINESS_TIMEZONE;
  }
}
