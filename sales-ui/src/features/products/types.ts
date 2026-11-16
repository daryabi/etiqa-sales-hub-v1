export interface Product {
  bookId: number;
  bookTitle: string;
  bookPrice: number;
  bookStockQuantity: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductImportResponse {
  totalRowsProcessed: number;
  importedCount: number;
  failedCount: number;
  errors: string[];
}

export interface InventorySummary {
  activeTitles: number;
  copiesInStock: number;
}

export interface NewProduct {
  bookTitle: string;
  bookPrice: number;
  bookStockQuantity: number;
}
