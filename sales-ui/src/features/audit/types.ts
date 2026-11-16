export type AuditedResource = 'CUSTOMER' | 'PRODUCT' | 'SALE';

export type AuditAction =
  | 'CREATE'
  | 'UPDATE'
  | 'PATCH'
  | 'DELETE'
  | 'RESTOCK'
  | 'BULK_IMPORT'
  | 'LOW_STOCK_ALERT';

export interface AuditLog {
  auditId: number;
  entityName: AuditedResource;
  action: AuditAction;
  entityId: number | null;
  performedBy: string;
  payload: string | null;
  createdAt: string;
}
