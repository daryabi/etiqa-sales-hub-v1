import { createCrudApi } from '../../api/createCrudApi';
import { fetchWithAuth, getJson, postJson, readJson } from '../../api/httpClient';
import type { InventorySummary, NewProduct, Product, ProductImportResponse } from './types';

const productCrud = createCrudApi<Product, NewProduct>('/products');

export const productsApi = {
  getProducts: productCrud.list,
  createProduct: productCrud.create,
  getInventorySummary: () => getJson<InventorySummary>('/products/summary'),
  restockProduct: (id: number, quantity: number) => postJson<Product>(`/products/${id}/restock`, { quantity }),
  importProductsFile: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return fetchWithAuth('/products/upload', { method: 'POST', body: formData }).then((response) =>
      readJson<ProductImportResponse>(response)
    );
  },
};
