IF OBJECT_ID(N'dbo.customers', N'U') IS NULL
CREATE TABLE customers (
    customer_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    first_name NVARCHAR(100) NOT NULL,
    last_name NVARCHAR(100) NOT NULL,
    email_office NVARCHAR(150) NULL,
    email_personal NVARCHAR(150) NULL,
    created_at DATETIME2(6) DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) DEFAULT SYSUTCDATETIME()
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ux_customers_email_office')
CREATE UNIQUE INDEX ux_customers_email_office ON customers (email_office) WHERE email_office IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ux_customers_email_personal')
CREATE UNIQUE INDEX ux_customers_email_personal ON customers (email_personal) WHERE email_personal IS NOT NULL;
