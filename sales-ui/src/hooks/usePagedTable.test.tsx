import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { Page, PageQuery } from '../types';
import { usePagedTable } from './usePagedTable';

function withQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

function pageOf(names: string[]): Page<{ name: string }> {
  return { content: names.map((name) => ({ name })), page: 0, size: 10, totalElements: names.length, totalPages: 1 };
}

describe('usePagedTable', () => {
  it('asks for the first page of 10 with the default sort', async () => {
    const fetchPage = vi.fn((_query: PageQuery) => Promise.resolve(pageOf(['Effective Java'])));

    const { result } = renderHook(
      () => usePagedTable({ queryKey: ['books'], fetchPage, defaultSorting: [{ id: 'bookId', desc: true }] }),
      { wrapper: withQueryClient() }
    );

    await waitFor(() => expect(result.current.tableProps.data).toHaveLength(1));
    expect(fetchPage).toHaveBeenCalledWith({ page: 0, size: 10, sort: 'bookId,desc', search: undefined });
    expect(result.current.tableProps.errorMessage).toBeNull();
  });

  it('sends the trimmed search term and goes back to the first page', async () => {
    const fetchPage = vi.fn(() => Promise.resolve(pageOf([])));
    const { result } = renderHook(
      () => usePagedTable({ queryKey: ['books'], fetchPage, defaultSorting: [] }),
      { wrapper: withQueryClient() }
    );

    act(() => result.current.tableProps.onPaginationChange({ pageIndex: 3, pageSize: 10 }));
    act(() => result.current.tableProps.onSearchChange('  java  '));

    await waitFor(() =>
      expect(fetchPage).toHaveBeenLastCalledWith({ page: 0, size: 10, sort: undefined, search: 'java' })
    );
  });

  it('exposes the error message when the page cannot be loaded', async () => {
    const fetchPage = vi.fn(() => Promise.reject(new Error('Request failed (503)')));
    const { result } = renderHook(
      () => usePagedTable({ queryKey: ['books'], fetchPage, defaultSorting: [] }),
      { wrapper: withQueryClient() }
    );

    await waitFor(() => expect(result.current.tableProps.errorMessage).toBe('Request failed (503)'));
  });
});
