import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { createColumnHelper, type ColumnDef } from '@tanstack/react-table';
import { ShoppingBag } from 'lucide-react';
import {
  Badge,
  Card,
  DataTable,
  DateTimeCell,
  Form,
  IdCell,
  Input,
  MoneyCell,
  PrimaryText,
  Select,
  SubmitButton,
} from '../../components/ui';
import { QUERY_KEYS } from '../../constants';
import { useApiMutation } from '../../hooks/useApiMutation';
import { useFormFields } from '../../hooks/useFormFields';
import { usePagedTable } from '../../hooks/usePagedTable';
import { formatMoney } from '../../utils/formatMoney';
import { customersApi } from '../customers/customersApi';
import { productsApi } from '../products/productsApi';
import { EMPTY_SALE_FORM, LOOKUP_QUERY } from './constants';
import { salesApi } from './salesApi';
import type { NewSale, Sale } from './types';

const columnHelper = createColumnHelper<Sale>();

export function SalesTab() {
  const { values: form, bind, reset } = useFormFields(EMPTY_SALE_FORM);
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());

  const { tableProps } = usePagedTable({
    queryKey: [QUERY_KEYS.SALES, 'table'],
    fetchPage: salesApi.getSales,
    defaultSorting: [{ id: 'saleDate', desc: true }],
  });

  const { data: customerPage } = useQuery({
    queryKey: [QUERY_KEYS.CUSTOMERS, 'lookup'],
    queryFn: () => customersApi.getCustomers({ ...LOOKUP_QUERY, sort: 'firstName,asc' }),
  });

  const { data: productPage } = useQuery({
    queryKey: [QUERY_KEYS.PRODUCTS, 'lookup'],
    queryFn: () => productsApi.getProducts({ ...LOOKUP_QUERY, sort: 'bookTitle,asc' }),
  });

  const recordSaleMutation = useApiMutation({
    mutationFn: (sale: NewSale) => salesApi.recordSale(sale, idempotencyKey),
    queryKeysToInvalidate: [[QUERY_KEYS.SALES], [QUERY_KEYS.PRODUCTS]],
    successMessage: 'Sale recorded and inventory deducted.',
    onSuccess: () => {
      reset();
      setIdempotencyKey(crypto.randomUUID());
    },
  });

  const customerOptions = useMemo(
    () =>
      (customerPage?.content ?? []).map((customer) => ({
        value: customer.customerId,
        label: `#${customer.customerId} — ${customer.firstName} ${customer.lastName}`,
      })),
    [customerPage]
  );

  const productOptions = useMemo(
    () =>
      (productPage?.content ?? []).map((product) => ({
        value: product.bookId,
        label: `${product.bookTitle} (${formatMoney(product.bookPrice)} • ${product.bookStockQuantity} left)`,
      })),
    [productPage]
  );

  const columns = useMemo(
    () =>
      [
        columnHelper.accessor('saleId', {
          header: 'ID',
          cell: (info) => <IdCell id={info.getValue()} />,
        }),
        columnHelper.accessor('bookTitle', {
          header: 'Book Title',
          enableSorting: false,
          cell: (info) => <PrimaryText>{info.getValue()}</PrimaryText>,
        }),
        columnHelper.accessor('customerName', { header: 'Customer', enableSorting: false }),
        columnHelper.accessor('salesQuantity', {
          header: 'Qty',
          cell: (info) => <Badge tone="brand">{info.getValue()}</Badge>,
        }),
        columnHelper.accessor('unitPrice', {
          id: 'salesPrice',
          header: 'Unit Price',
          cell: (info) => <MoneyCell amount={info.getValue()} />,
        }),
        columnHelper.accessor('totalAmount', {
          header: 'Total',
          enableSorting: false,
          cell: (info) => <MoneyCell amount={info.getValue()} highlight />,
        }),
        columnHelper.accessor('saleDate', {
          header: 'Date (MYT)',
          cell: (info) => <DateTimeCell value={info.getValue()} />,
        }),
      ] as ColumnDef<Sale, unknown>[],
    []
  );

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <Card title="New Sale Transaction" icon={<ShoppingBag className="w-4 h-4" />} className="h-fit">
        <Form
          onSubmit={() =>
            recordSaleMutation.mutate({
              customerId: Number(form.customerId),
              bookId: Number(form.bookId),
              salesQuantity: Number(form.salesQuantity),
            })
          }
        >
          <Select
            required
            label="Select Customer"
            placeholder="-- Choose Customer --"
            options={customerOptions}
            {...bind('customerId')}
          />
          <Select
            required
            label="Select Book"
            placeholder="-- Choose Book --"
            options={productOptions}
            {...bind('bookId')}
          />
          <Input
            required
            type="number"
            min={1}
            label="Quantity"
            {...bind('salesQuantity')}
          />
          <SubmitButton isPending={recordSaleMutation.isPending} loadingText="Processing...">
            Complete Sale & Deduct Stock
          </SubmitButton>
        </Form>
      </Card>

      <div className="lg:col-span-2">
        <DataTable {...tableProps} columns={columns} searchPlaceholder="Search sales by book or customer..." />
      </div>
    </div>
  );
}
