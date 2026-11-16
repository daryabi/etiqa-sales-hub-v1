IF OBJECT_ID(N'dbo.users', N'U') IS NULL
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL,
    password_hash NVARCHAR(100) NOT NULL,
    role NVARCHAR(20) NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'STAFF'))
);
