IF OBJECT_ID(N'dbo.audit_log', N'U') IS NULL
CREATE TABLE audit_log (
    audit_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    entity_name NVARCHAR(50) NOT NULL,
    action NVARCHAR(30) NOT NULL,
    entity_id BIGINT NULL,
    performed_by NVARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    payload NVARCHAR(MAX) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME()
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_audit_log_created_at')
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_audit_log_entity')
CREATE INDEX idx_audit_log_entity ON audit_log (entity_name, entity_id);
