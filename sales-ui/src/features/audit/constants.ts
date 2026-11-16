import type { AuditedResource } from './types';

export const AUDIT_LOG_FILTER_OPTIONS: { value: AuditedResource; label: string }[] = [
  { value: 'CUSTOMER', label: 'Customers' },
  { value: 'PRODUCT', label: 'Products' },
  { value: 'SALE', label: 'Sales' },
];
