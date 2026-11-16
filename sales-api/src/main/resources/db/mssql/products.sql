IF OBJECT_ID(N'dbo.products', N'U') IS NULL
CREATE TABLE products (
    book_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    book_title NVARCHAR(255) NOT NULL,
    book_price DECIMAL(10, 2) NOT NULL,
    book_stock_quantity INT NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME2(6) DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) DEFAULT SYSUTCDATETIME(),
    CONSTRAINT chk_stock_non_negative CHECK (book_stock_quantity >= 0),
    CONSTRAINT chk_price_positive CHECK (book_price > 0)
);
