package com.etiqa.sales.product;

import java.util.List;

public record ProductImportResponse(
        int totalRowsProcessed,
        int importedCount,
        int failedCount,
        List<String> errors
) {
    public ProductImportResponse(int totalRowsProcessed, int importedCount, int failedCount, List<String> errors) {
        this.totalRowsProcessed = totalRowsProcessed;
        this.importedCount = importedCount;
        this.failedCount = failedCount;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
