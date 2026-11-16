CREATE TABLE IF NOT EXISTS products (
    book_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_title VARCHAR(255) NOT NULL,
    book_price DECIMAL(10, 2) NOT NULL,
    book_stock_quantity INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_stock_non_negative CHECK (book_stock_quantity >= 0),
    CONSTRAINT chk_price_positive CHECK (book_price > 0)
);
