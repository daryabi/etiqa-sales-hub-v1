import { APP_CONFIG } from '../constants';
import { getBusinessTimezone } from './businessTimezone';

const formattersByTimezone = new Map<string, Intl.DateTimeFormat>();

function formatterFor(timeZone: string): Intl.DateTimeFormat {
  let formatter = formattersByTimezone.get(timeZone);
  if (!formatter) {
    formatter = new Intl.DateTimeFormat(APP_CONFIG.APP_LOCALE, {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    });
    formattersByTimezone.set(timeZone, formatter);
  }
  return formatter;
}

export function formatDateTime(isoTimestamp: string, timeZone: string = getBusinessTimezone()): string {
  return formatterFor(timeZone).format(new Date(isoTimestamp));
}
