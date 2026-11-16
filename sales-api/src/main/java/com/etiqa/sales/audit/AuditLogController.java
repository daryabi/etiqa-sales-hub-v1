package com.etiqa.sales.audit;

import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Activity and audit trail endpoints for system mutations")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Retrieve system activity and audit logs, newest first")
    public ApiResponse<PageResponse<AuditLogResponse>> getAuditLogs(
            @Parameter(description = "Optional domain module filter")
            @RequestParam(required = false) AuditedResource entityName,
            @ParameterObject
            @PageableDefault
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "auditId", direction = Sort.Direction.DESC)
            })
            Pageable pageable
    ) {
        return ApiResponse.ok(auditLogService.findAll(Optional.ofNullable(entityName), pageable));
    }
}
