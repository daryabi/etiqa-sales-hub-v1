export interface Sale {
  saleId: number;
  saleDate: string;
  bookId: number;
  bookTitle: string;
  customerId: number;
  customerName: string;
  salesQuantity: number;
  unitPrice: number;
  totalAmount: number;
}

export interface SalesSummary {
  orderCount: number;
  unitsSold: number;
  revenue: number;
}

export interface NewSale {
  customerId: number;
  bookId: number;
  salesQuantity: number;
}
