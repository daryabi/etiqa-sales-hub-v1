CREATE TABLE IF NOT EXISTS sales (
    sale_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    book_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    sales_quantity INT NOT NULL,
    sales_price DECIMAL(10, 2) NOT NULL,
    INDEX idx_sales_date (sale_date),
    CONSTRAINT fk_sales_product FOREIGN KEY (book_id) REFERENCES products(book_id),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT chk_sales_qty_positive CHECK (sales_quantity > 0)
);
