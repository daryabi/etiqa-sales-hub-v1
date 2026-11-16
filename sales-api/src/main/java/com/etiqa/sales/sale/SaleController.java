package com.etiqa.sales.sale;

import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales", description = "Endpoints for recording sales and querying transactions")
public class SaleController {

    private final SaleService saleService;
    private final SaleIdempotencyGuard saleIdempotencyGuard;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a new sale and deduct product stock. Send an Idempotency-Key header to make retries safe.")
    public ApiResponse<SaleResponse> recordSale(
            @Valid @RequestBody SaleRequest request,
            @RequestHeader(value = SaleIdempotencyGuard.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            Principal principal
    ) {
        return ApiResponse.of("Sale recorded", saleIdempotencyGuard.recordOnce(principal.getName(), idempotencyKey, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing sale and reconcile product stock")
    public ApiResponse<SaleResponse> replaceSale(
            @PathVariable Long id,
            @Valid @RequestBody SaleRequest request
    ) {
        return ApiResponse.of("Sale updated", saleService.replaceSale(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an existing sale and reconcile product stock")
    public ApiResponse<SaleResponse> patchSale(
            @PathVariable Long id,
            @Valid @RequestBody SalePatchRequest request
    ) {
        return ApiResponse.of("Sale updated", saleService.patchSale(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a sale and put its quantity back in stock (admin only)")
    public void deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a sale transaction by ID")
    public ApiResponse<SaleResponse> getSale(@PathVariable Long id) {
        return ApiResponse.ok(saleService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List sale transactions page by page, optionally filtered by book title or customer name")
    public ApiResponse<PageResponse<SaleResponse>> getSales(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(saleService.getSales(search, pageable));
    }

    @GetMapping("/summary")
    @Operation(summary = "Total orders, units sold, and revenue across all sales")
    public ApiResponse<SalesSummaryResponse> getSalesSummary() {
        return ApiResponse.ok(saleService.summarize());
    }
}
