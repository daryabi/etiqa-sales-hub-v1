import { useMemo } from 'react';
import { createColumnHelper, type ColumnDef } from '@tanstack/react-table';
import { UserPlus } from 'lucide-react';
import { Card, DataTable, Form, IdCell, Input, PrimaryText, SubmitButton } from '../../components/ui';
import { QUERY_KEYS } from '../../constants';
import { useApiMutation } from '../../hooks/useApiMutation';
import { useFormFields } from '../../hooks/useFormFields';
import { usePagedTable } from '../../hooks/usePagedTable';
import { orDash } from '../../utils/orDash';
import { EMPTY_CUSTOMER_FORM } from './constants';
import { customersApi } from './customersApi';
import type { Customer } from './types';

const columnHelper = createColumnHelper<Customer>();

export function CustomersTab() {
  const { values: form, bind, reset } = useFormFields(EMPTY_CUSTOMER_FORM);

  const { tableProps } = usePagedTable({
    queryKey: [QUERY_KEYS.CUSTOMERS, 'table'],
    fetchPage: customersApi.getCustomers,
    defaultSorting: [{ id: 'customerId', desc: true }],
  });

  const createMutation = useApiMutation({
    mutationFn: customersApi.createCustomer,
    queryKeysToInvalidate: [[QUERY_KEYS.CUSTOMERS]],
    successMessage: 'Customer registered successfully.',
    onSuccess: reset,
  });

  const columns = useMemo(
    () =>
      [
        columnHelper.accessor('customerId', {
          header: 'ID',
          cell: (info) => <IdCell id={info.getValue()} />,
        }),
        columnHelper.accessor((row) => `${row.firstName} ${row.lastName}`, {
          id: 'firstName',
          header: 'Customer Name',
          cell: (info) => <PrimaryText>{info.getValue()}</PrimaryText>,
        }),
        columnHelper.accessor('emailOffice', {
          header: 'Office Email',
          cell: (info) => orDash(info.getValue()),
        }),
        columnHelper.accessor('emailPersonal', {
          header: 'Personal Email',
          cell: (info) => orDash(info.getValue()),
        }),
      ] as ColumnDef<Customer, unknown>[],
    []
  );

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <Card title="Register Customer" icon={<UserPlus className="w-4 h-4" />} className="h-fit">
        <Form
          onSubmit={() =>
            createMutation.mutate({
              firstName: form.firstName,
              lastName: form.lastName,
              emailOffice: form.emailOffice || null,
              emailPersonal: form.emailPersonal || null,
            })
          }
        >
          <Input
            required
            placeholder="First Name"
            {...bind('firstName')}
          />
          <Input
            required
            placeholder="Last Name"
            {...bind('lastName')}
          />
          <Input
            type="email"
            placeholder="Office Email (optional)"
            {...bind('emailOffice')}
          />
          <Input
            type="email"
            placeholder="Personal Email (optional)"
            {...bind('emailPersonal')}
          />
          <SubmitButton isPending={createMutation.isPending}>Save Customer</SubmitButton>
        </Form>
      </Card>

      <div className="lg:col-span-2">
        <DataTable {...tableProps} columns={columns} searchPlaceholder="Search customers by name or email..." />
      </div>
    </div>
  );
}
