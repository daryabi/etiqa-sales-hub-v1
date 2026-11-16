import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationProvider } from '../../notifications/NotificationProvider';
import { SalesTab } from './SalesTab';
import { salesApi } from './salesApi';

vi.mock('./salesApi', () => ({
  salesApi: {
    getSales: vi.fn(),
    recordSale: vi.fn(),
  },
}));

vi.mock('../customers/customersApi', () => ({
  customersApi: {
    getCustomers: vi.fn(() =>
      Promise.resolve({
        content: [{ customerId: 7, firstName: 'Aisyah', lastName: 'Rahman' }],
        page: 0, size: 100, totalElements: 1, totalPages: 1,
      })
    ),
  },
}));

vi.mock('../products/productsApi', () => ({
  productsApi: {
    getProducts: vi.fn(() =>
      Promise.resolve({
        content: [{ bookId: 3, bookTitle: 'Effective Java', bookPrice: 45.5, bookStockQuantity: 20 }],
        page: 0, size: 100, totalElements: 1, totalPages: 1,
      })
    ),
  },
}));

function renderSalesTab() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <NotificationProvider>
        <SalesTab />
      </NotificationProvider>
    </QueryClientProvider>
  );
}

async function checkout() {
  await screen.findByRole('option', { name: /Aisyah Rahman/ });
  await screen.findByRole('option', { name: /Effective Java/ });
  fireEvent.change(screen.getByLabelText('Select Customer'), { target: { value: '7' } });
  fireEvent.change(screen.getByLabelText('Select Book'), { target: { value: '3' } });
  fireEvent.change(screen.getByLabelText('Quantity'), { target: { value: '2' } });
  fireEvent.click(screen.getByRole('button', { name: /Complete Sale/ }));
}

beforeEach(() => {
  vi.mocked(salesApi.getSales).mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  vi.mocked(salesApi.recordSale).mockResolvedValue({ saleId: 1 } as never);
  vi.spyOn(crypto, 'randomUUID')
    .mockReturnValueOnce('11111111-1111-4111-8111-111111111111')
    .mockReturnValueOnce('22222222-2222-4222-8222-222222222222');
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe('SalesTab checkout', () => {
  it('uses a fresh Idempotency-Key for the next sale once one succeeds', async () => {
    renderSalesTab();

    await checkout();
    await screen.findByText('Sale recorded and inventory deducted.');
    await checkout();

    await waitFor(() => expect(salesApi.recordSale).toHaveBeenCalledTimes(2));
    const sale = { customerId: 7, bookId: 3, salesQuantity: 2 };
    expect(salesApi.recordSale).toHaveBeenNthCalledWith(1, sale, '11111111-1111-4111-8111-111111111111');
    expect(salesApi.recordSale).toHaveBeenNthCalledWith(2, sale, '22222222-2222-4222-8222-222222222222');
  });

  it('keeps the same key when a sale fails so a retry stays safe', async () => {
    vi.mocked(salesApi.recordSale).mockRejectedValueOnce(new Error('Network down'));
    renderSalesTab();

    await checkout();
    await screen.findByText('Network down');
    await checkout();

    await waitFor(() => expect(salesApi.recordSale).toHaveBeenCalledTimes(2));
    const keys = vi.mocked(salesApi.recordSale).mock.calls.map(([, key]) => key);
    expect(keys).toEqual(['11111111-1111-4111-8111-111111111111', '11111111-1111-4111-8111-111111111111']);
  });
});
