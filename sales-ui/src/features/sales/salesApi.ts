import { createCrudApi } from '../../api/createCrudApi';
import { getJson, postJson } from '../../api/httpClient';
import type { NewSale, Sale, SalesSummary } from './types';

const saleCrud = createCrudApi<Sale, NewSale>('/sales');

export const salesApi = {
  getSales: saleCrud.list,
  recordSale: (sale: NewSale, idempotencyKey: string) =>
    postJson<Sale>('/sales', sale, { 'Idempotency-Key': idempotencyKey }),
  getSalesSummary: () => getJson<SalesSummary>('/sales/summary'),
};
