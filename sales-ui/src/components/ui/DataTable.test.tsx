import { createColumnHelper, type ColumnDef } from '@tanstack/react-table';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DataTable } from './DataTable';

interface Book {
  bookTitle: string;
}

const columns = [createColumnHelper<Book>().accessor('bookTitle', { header: 'Title' })] as ColumnDef<Book, unknown>[];

function renderTable(overrides: { data?: Book[]; errorMessage?: string | null }) {
  return render(
    <DataTable<Book>
      data={overrides.data ?? []}
      columns={columns}
      totalElements={overrides.data?.length ?? 0}
      pageCount={1}
      pagination={{ pageIndex: 0, pageSize: 10 }}
      onPaginationChange={() => undefined}
      sorting={[]}
      onSortingChange={() => undefined}
      search=""
      onSearchChange={() => undefined}
      errorMessage={overrides.errorMessage}
    />
  );
}

describe('DataTable', () => {
  it('shows the load error instead of pretending there is no data', () => {
    renderTable({ errorMessage: 'Request failed (503)' });

    expect(screen.getByText('Could not load records: Request failed (503)')).toBeTruthy();
    expect(screen.queryByText('No matching records found.')).toBeNull();
  });

  it('shows the empty state when the page really is empty', () => {
    renderTable({});

    expect(screen.getByText('No matching records found.')).toBeTruthy();
  });

  it('renders a row for each record', () => {
    renderTable({ data: [{ bookTitle: 'Effective Java' }, { bookTitle: 'Refactoring' }] });

    expect(screen.getByText('Effective Java')).toBeTruthy();
    expect(screen.getByText('Refactoring')).toBeTruthy();
  });
});
