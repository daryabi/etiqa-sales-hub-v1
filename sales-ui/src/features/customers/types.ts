export interface Customer {
  customerId: number;
  firstName: string;
  lastName: string;
  emailOffice: string | null;
  emailPersonal: string | null;
  createdAt: string;
  updatedAt: string;
}

export type NewCustomer = Omit<Customer, 'customerId' | 'createdAt' | 'updatedAt'>;
