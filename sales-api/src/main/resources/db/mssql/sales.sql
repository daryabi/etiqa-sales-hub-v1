IF OBJECT_ID(N'dbo.sales', N'U') IS NULL
CREATE TABLE sales (
    sale_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sale_date DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    book_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    sales_quantity INT NOT NULL,
    sales_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_sales_product FOREIGN KEY (book_id) REFERENCES products (book_id),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT chk_sales_qty_positive CHECK (sales_quantity > 0)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_sales_date')
CREATE INDEX idx_sales_date ON sales (sale_date);
