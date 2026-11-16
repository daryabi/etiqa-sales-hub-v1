package com.etiqa.sales.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit log entry capturing a create, update, patch, or bulk import activity")
public record AuditLogResponse(
        @Schema(description = "Unique audit log ID", example = "1")
        Long auditId,

        @Schema(description = "Domain entity module name", example = "SALE")
        String entityName,

        @Schema(description = "Operation type performed", example = "CREATE")
        String action,

        @Schema(description = "Primary key ID of the affected record (null for bulk imports)", example = "12")
        Long entityId,

        @Schema(description = "Actor or service principal that triggered the action", example = "SYSTEM")
        String performedBy,

        @Schema(description = "JSON snapshot of the recorded mutation payload")
        String payload,

        @Schema(description = "UTC timestamp when the audit entry was persisted")
        Instant createdAt
) {
}
