import { getJson, pageQueryString } from '../../api/httpClient';
import type { Page, PageQuery } from '../../types';
import type { AuditedResource, AuditLog } from './types';

export const auditApi = {
  getAuditLogs: (query: PageQuery, entityName?: AuditedResource) =>
    getJson<Page<AuditLog>>(`/audit-logs?${pageQueryString(query, { entityName })}`),
};
