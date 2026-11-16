CREATE TABLE IF NOT EXISTS audit_log (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    action VARCHAR(30) NOT NULL,
    entity_id BIGINT NULL,
    performed_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    payload JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_log_created_at (created_at),
    INDEX idx_audit_log_entity (entity_name, entity_id)
);
