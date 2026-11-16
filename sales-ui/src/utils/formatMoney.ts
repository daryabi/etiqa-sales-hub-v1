import { APP_CONFIG } from '../constants';

const myrFormatter = new Intl.NumberFormat(APP_CONFIG.APP_LOCALE, {
  style: 'currency',
  currency: APP_CONFIG.CURRENCY_CODE,
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatMoney(amount: number | string): string {
  return myrFormatter.format(Number(amount));
}
