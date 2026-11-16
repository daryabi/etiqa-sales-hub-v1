import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
  type OnChangeFn,
  type PaginationState,
  type SortingState,
} from '@tanstack/react-table';
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, Search } from 'lucide-react';
import { Button } from './Button';
import { Input } from './Input';

interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T, unknown>[];
  totalElements: number;
  pageCount: number;
  pagination: PaginationState;
  onPaginationChange: OnChangeFn<PaginationState>;
  sorting: SortingState;
  onSortingChange: OnChangeFn<SortingState>;
  search: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder?: string;
  showSearch?: boolean;
  isLoading?: boolean;
  errorMessage?: string | null;
}

export function DataTable<T>({
  data,
  columns,
  totalElements,
  pageCount,
  pagination,
  onPaginationChange,
  sorting,
  onSortingChange,
  search,
  onSearchChange,
  searchPlaceholder = 'Search records...',
  showSearch = true,
  isLoading = false,
  errorMessage = null,
}: DataTableProps<T>) {
  const table = useReactTable({
    data,
    columns,
    pageCount,
    state: { pagination, sorting },
    onPaginationChange,
    onSortingChange,
    manualPagination: true,
    manualSorting: true,
    enableMultiSort: false,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="w-full sm:w-80">
          {showSearch && (
            <Input
              value={search}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder={searchPlaceholder}
              leftIcon={<Search className="w-4 h-4" />}
            />
          )}
        </div>
        <span className="text-xs font-medium text-muted bg-white px-3.5 py-2 rounded-full border border-gray-200">
          {isLoading ? 'Loading...' : (
            <>
              Showing <strong className="text-ink">{data.length}</strong> of{' '}
              <strong className="text-ink">{totalElements}</strong> entries
            </>
          )}
        </span>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white shadow-xs">
        <table className="w-full text-left border-collapse">
          <thead>
            {table.getHeaderGroups().map((group) => (
              <tr key={group.id} className="border-b-2 border-brand bg-ink">
                {group.headers.map((header) => {
                  const canSort = header.column.getCanSort();
                  const sortDirection = header.column.getIsSorted();
                  return (
                    <th
                      key={header.id}
                      onClick={canSort ? header.column.getToggleSortingHandler() : undefined}
                      className={`py-3.5 px-4 text-xs font-bold uppercase tracking-wider text-white select-none transition ${
                        canSort ? 'cursor-pointer hover:text-brand' : ''
                      }`}
                    >
                      <div className="flex items-center gap-1.5">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {canSort && sortDirection === 'asc' && <ArrowUp className="w-3.5 h-3.5 text-brand" />}
                        {canSort && sortDirection === 'desc' && <ArrowDown className="w-3.5 h-3.5 text-brand" />}
                        {canSort && !sortDirection && <ArrowUpDown className="w-3.5 h-3.5 text-brand/60" />}
                      </div>
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>
          <tbody className="divide-y divide-gray-100 text-sm">
            {errorMessage ? (
              <tr>
                <td colSpan={columns.length} className="py-12 text-center text-rose-600">
                  Could not load records: {errorMessage}
                </td>
              </tr>
            ) : table.getRowModel().rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="py-12 text-center text-muted">
                  No matching records found.
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr key={row.id} className="hover:bg-brand-tint/70 transition-colors">
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="py-3.5 px-4 text-ink">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {pageCount > 1 && (
        <div className="flex items-center justify-between text-xs text-muted pt-1">
          <span>
            Page <strong className="text-ink">{pagination.pageIndex + 1}</strong> of{' '}
            <strong className="text-ink">{pageCount}</strong>
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
              icon={<ChevronLeft className="w-4 h-4" />}
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
              icon={<ChevronRight className="w-4 h-4" />}
            />
          </div>
        </div>
      )}
    </div>
  );
}
