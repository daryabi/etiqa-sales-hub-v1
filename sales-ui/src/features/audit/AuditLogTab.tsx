import { useCallback, useMemo, useState } from 'react';
import { createColumnHelper, type ColumnDef } from '@tanstack/react-table';
import { History, RefreshCw } from 'lucide-react';
import { Badge, Button, Card, DataTable, DateTimeCell, IdCell, PrimaryText, Select, type BadgeTone } from '../../components/ui';
import { QUERY_KEYS } from '../../constants';
import { usePagedTable } from '../../hooks/usePagedTable';
import type { PageQuery } from '../../types';
import { auditApi } from './auditApi';
import { AUDIT_LOG_FILTER_OPTIONS } from './constants';
import type { AuditAction, AuditedResource, AuditLog } from './types';

const columnHelper = createColumnHelper<AuditLog>();

function actionBadgeTone(action: AuditAction): BadgeTone {
  if (action === 'CREATE' || action === 'RESTOCK') return 'success';
  if (action === 'BULK_IMPORT') return 'brand';
  if (action === 'DELETE' || action === 'LOW_STOCK_ALERT') return 'danger';
  return 'warning';
}

export function AuditLogTab() {
  const [entityFilter, setEntityFilter] = useState<AuditedResource | ''>('');

  const fetchAuditPage = useCallback(
    (query: PageQuery) => auditApi.getAuditLogs(query, entityFilter || undefined),
    [entityFilter]
  );

  const { query, resetPage, tableProps } = usePagedTable({
    queryKey: [QUERY_KEYS.AUDIT_LOGS, entityFilter],
    fetchPage: fetchAuditPage,
    defaultSorting: [{ id: 'createdAt', desc: true }],
  });

  const columns = useMemo(
    () =>
      [
        columnHelper.accessor('auditId', {
          header: 'Log ID',
          cell: (info) => <IdCell id={info.getValue()} />,
        }),
        columnHelper.accessor('entityName', {
          header: 'Module',
          cell: (info) => <Badge tone="neutral">{info.getValue()}</Badge>,
        }),
        columnHelper.accessor('action', {
          header: 'Action',
          cell: (info) => {
            const action = info.getValue();
            return <Badge tone={actionBadgeTone(action)}>{action}</Badge>;
          },
        }),
        columnHelper.accessor('entityId', {
          header: 'Record ID',
          cell: (info) => <IdCell id={info.getValue()} />,
        }),
        columnHelper.accessor('performedBy', {
          header: 'Actor',
          cell: (info) => <PrimaryText>{info.getValue()}</PrimaryText>,
        }),
        columnHelper.accessor('payload', {
          header: 'Change Snapshot (JSON)',
          enableSorting: false,
          cell: (info) => {
            const payloadJson = info.getValue();
            if (!payloadJson) return '—';
            return (
              <code
                title={payloadJson}
                className="block max-w-xs truncate rounded-lg bg-canvas border border-gray-200 px-2.5 py-1 text-xs font-mono text-ink"
              >
                {payloadJson}
              </code>
            );
          },
        }),
        columnHelper.accessor('createdAt', {
          header: 'Timestamp (MYT)',
          cell: (info) => <DateTimeCell value={info.getValue()} />,
        }),
      ] as ColumnDef<AuditLog, unknown>[],
    []
  );

  return (
    <Card
      title="System Activity & Audit Log"
      subtitle="Chronological audit trail of customer, catalog, and POS operations"
      icon={<History className="w-4 h-4" />}
      headerAction={
        <div className="flex flex-wrap items-center gap-3">
          <div className="w-44">
            <Select
              placeholder="All Modules"
              options={AUDIT_LOG_FILTER_OPTIONS}
              value={entityFilter}
              onChange={(event) => {
                setEntityFilter(event.target.value as AuditedResource | '');
                resetPage();
              }}
            />
          </div>
          <Button
            variant="outline"
            size="md"
            onClick={() => query.refetch()}
            isLoading={query.isFetching}
            icon={<RefreshCw className={`w-4 h-4 ${query.isFetching ? 'animate-spin' : ''}`} />}
          >
            Refresh Log
          </Button>
        </div>
      }
    >
      <DataTable {...tableProps} columns={columns} showSearch={false} />
    </Card>
  );
}
