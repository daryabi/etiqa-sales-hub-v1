import { useMemo, useState } from 'react';
import { createColumnHelper, type ColumnDef } from '@tanstack/react-table';
import { FileSpreadsheet, PackagePlus, Plus, Upload } from 'lucide-react';
import { Badge, Button, Card, DataTable, Form, IdCell, Input, MoneyCell, PrimaryText, SubmitButton } from '../../components/ui';
import { QUERY_KEYS } from '../../constants';
import { useApiMutation } from '../../hooks/useApiMutation';
import { useFormFields } from '../../hooks/useFormFields';
import { usePagedTable } from '../../hooks/usePagedTable';
import { EMPTY_BOOK_FORM, LOW_STOCK_THRESHOLD, RESTOCK_INCREMENT } from './constants';
import { productsApi } from './productsApi';
import type { Product, ProductImportResponse } from './types';

const columnHelper = createColumnHelper<Product>();

export function ProductsTab() {
  const { values: form, bind, reset } = useFormFields(EMPTY_BOOK_FORM);
  const [importResult, setImportResult] = useState<ProductImportResponse | null>(null);

  const { tableProps } = usePagedTable({
    queryKey: [QUERY_KEYS.PRODUCTS, 'table'],
    fetchPage: productsApi.getProducts,
    defaultSorting: [{ id: 'bookId', desc: true }],
  });

  const createMutation = useApiMutation({
    mutationFn: productsApi.createProduct,
    queryKeysToInvalidate: [[QUERY_KEYS.PRODUCTS]],
    successMessage: 'Book added to catalog.',
    onSuccess: reset,
  });

  const restockMutation = useApiMutation({
    mutationFn: (bookId: number) => productsApi.restockProduct(bookId, RESTOCK_INCREMENT),
    queryKeysToInvalidate: [[QUERY_KEYS.PRODUCTS]],
    successMessage: 'Inventory stock updated.',
  });

  const importMutation = useApiMutation({
    mutationFn: productsApi.importProductsFile,
    queryKeysToInvalidate: [[QUERY_KEYS.PRODUCTS]],
    successMessage: (result) => `${result.importedCount} / ${result.totalRowsProcessed} imported`,
    onSuccess: setImportResult,
  });

  const restockBook = restockMutation.mutate;

  const columns = useMemo(
    () =>
      [
        columnHelper.accessor('bookId', {
          header: 'ID',
          cell: (info) => <IdCell id={info.getValue()} />,
        }),
        columnHelper.accessor('bookTitle', {
          header: 'Title',
          cell: (info) => <PrimaryText>{info.getValue()}</PrimaryText>,
        }),
        columnHelper.accessor('bookPrice', {
          header: 'Price',
          cell: (info) => <MoneyCell amount={info.getValue()} />,
        }),
        columnHelper.accessor('bookStockQuantity', {
          header: 'Stock',
          cell: (info) => {
            const stockQuantity = info.getValue();
            const tone =
              stockQuantity === 0
                ? 'danger'
                : stockQuantity < LOW_STOCK_THRESHOLD
                  ? 'warning'
                  : 'success';
            return <Badge tone={tone}>{stockQuantity} in stock</Badge>;
          },
        }),
        columnHelper.display({
          id: 'actions',
          header: 'Quick Restock',
          cell: (info) => (
            <Button variant="outline" size="sm" onClick={() => restockBook(info.row.original.bookId)}>
              +{RESTOCK_INCREMENT} Stock
            </Button>
          ),
        }),
      ] as ColumnDef<Product, unknown>[],
    [restockBook]
  );

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div className="space-y-6">
        <Card title="Add Single Book" icon={<PackagePlus className="w-4 h-4" />}>
          <Form
            onSubmit={() =>
              createMutation.mutate({
                bookTitle: form.bookTitle,
                bookPrice: Number(form.bookPrice),
                bookStockQuantity: Number(form.bookStockQuantity),
              })
            }
          >
            <Input
              required
              placeholder="Book Title (e.g., Effective Java)"
              {...bind('bookTitle')}
            />
            <div className="grid grid-cols-2 gap-3">
              <Input
                required
                type="number"
                step={0.01}
                min={0.01}
                placeholder="Price (RM)"
                {...bind('bookPrice')}
              />
              <Input
                required
                type="number"
                min={0}
                placeholder="Initial Stock"
                {...bind('bookStockQuantity')}
              />
            </div>
            <SubmitButton isPending={createMutation.isPending} icon={<Plus className="w-4 h-4" />}>
              Save Book
            </SubmitButton>
          </Form>
        </Card>

        <Card
          title="Bulk Pipe (|) File Upload"
          icon={<FileSpreadsheet className="w-4 h-4" />}
          subtitle="Format: bookTitle|bookPrice|bookStockQuantity"
        >
          <label className="flex flex-col items-center justify-center p-6 border-2 border-dashed border-gray-300 hover:border-brand rounded-2xl cursor-pointer bg-canvas hover:bg-brand-tint/60 transition">
            <Upload className="w-6 h-6 text-ink mb-2" />
            <span className="text-xs font-bold text-ink">Click to select .txt or .csv</span>
            <input
              type="file"
              accept=".txt,.csv"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) importMutation.mutate(file);
                event.target.value = '';
              }}
            />
          </label>
          {importResult && (
            <div className="p-3.5 rounded-xl bg-canvas border border-gray-200 text-xs space-y-1">
              <p className="text-emerald-700 font-bold">
                {importResult.importedCount} / {importResult.totalRowsProcessed} imported
              </p>
              {importResult.errors.map((message) => (
                <p key={message} className="text-rose-600">
                  {message}
                </p>
              ))}
            </div>
          )}
        </Card>
      </div>

      <div className="lg:col-span-2">
        <DataTable {...tableProps} columns={columns} searchPlaceholder="Filter books by title..." />
      </div>
    </div>
  );
}
