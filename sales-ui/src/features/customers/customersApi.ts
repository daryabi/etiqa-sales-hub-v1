import { createCrudApi } from '../../api/createCrudApi';
import type { Customer, NewCustomer } from './types';

const customerCrud = createCrudApi<Customer, NewCustomer>('/customers');

export const customersApi = {
  getCustomers: customerCrud.list,
  createCustomer: customerCrud.create,
};
