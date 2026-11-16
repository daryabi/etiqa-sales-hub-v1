import { useCallback, useState } from 'react';
import { keepPreviousData, useQuery, type QueryKey } from '@tanstack/react-query';
import type { OnChangeFn, PaginationState, SortingState } from '@tanstack/react-table';
import { APP_CONFIG } from '../constants';
import type { Page, PageQuery } from '../types';
import { useDebouncedValue } from './useDebouncedValue';

interface UsePagedTableOptions<T> {
  queryKey: QueryKey;
  fetchPage: (query: PageQuery) => Promise<Page<T>>;
  defaultSorting: SortingState;
  pageSize?: number;
}

export function usePagedTable<T>({
  queryKey,
  fetchPage,
  defaultSorting,
  pageSize = APP_CONFIG.PAGE_SIZE,
}: UsePagedTableOptions<T>) {
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize });
  const [sorting, setSortingState] = useState<SortingState>(defaultSorting);
  const [search, setSearchText] = useState('');
  const debouncedSearch = useDebouncedValue(search.trim(), APP_CONFIG.SEARCH_DEBOUNCE_MS);

  const sortParam = sorting.map((rule) => `${rule.id},${rule.desc ? 'desc' : 'asc'}`)[0];

  const query = useQuery({
    queryKey: [...queryKey, pagination.pageIndex, pagination.pageSize, sortParam, debouncedSearch],
    queryFn: () =>
      fetchPage({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        sort: sortParam,
        search: debouncedSearch || undefined,
      }),
    placeholderData: keepPreviousData,
  });

  const resetPage = useCallback(() => {
    setPagination((current) => ({ ...current, pageIndex: 0 }));
  }, []);

  const setSorting: OnChangeFn<SortingState> = useCallback(
    (updater) => {
      setSortingState(updater);
      resetPage();
    },
    [resetPage]
  );

  const setSearch = useCallback(
    (value: string) => {
      setSearchText(value);
      resetPage();
    },
    [resetPage]
  );

  return {
    query,
    resetPage,
    tableProps: {
      data: query.data?.content ?? [],
      totalElements: query.data?.totalElements ?? 0,
      pageCount: query.data?.totalPages ?? 0,
      pagination,
      onPaginationChange: setPagination,
      sorting,
      onSortingChange: setSorting,
      search,
      onSearchChange: setSearch,
      isLoading: query.isFetching,
      errorMessage: query.isError ? query.error.message : null,
    },
  };
}
