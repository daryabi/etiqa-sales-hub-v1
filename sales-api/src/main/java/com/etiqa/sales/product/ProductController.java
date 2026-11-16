package com.etiqa.sales.product;

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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for managing books, stock inventory, and bulk uploads")
public class ProductController {

    private final ProductService productService;
    private final ProductImportService productImportService;
    private final BookLookupService bookLookupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new book product")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.of("Book added", productService.create(request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk import book products from a pipe-delimited (|) text/CSV file")
    public ApiResponse<ProductImportResponse> importProducts(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of("Books imported", productImportService.importProducts(file));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a book product by ID")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List active book products page by page, optionally filtered by title")
    public ApiResponse<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "bookId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(productService.getActiveProducts(search, pageable));
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Look up a book's details by ISBN from Open Library (signed-in users only)")
    public ApiResponse<BookLookupResponse> lookUpByIsbn(@PathVariable String isbn) {
        return ApiResponse.ok(bookLookupService.lookUpByIsbn(isbn));
    }

    @GetMapping("/summary")
    @Operation(summary = "Count active titles and copies in stock")
    public ApiResponse<InventorySummaryResponse> getInventorySummary() {
        return ApiResponse.ok(productService.summarizeInventory());
    }

    @PostMapping("/{id}/restock")
    @Operation(summary = "Atomically add stock to a book product")
    public ApiResponse<ProductResponse> restockProduct(
            @PathVariable Long id,
            @Valid @RequestBody RestockRequest request
    ) {
        return ApiResponse.of("Stock added", productService.restock(id, request.quantity()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing book product")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.of("Book updated", productService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update book price, stock, or active flag")
    public ApiResponse<ProductResponse> patchProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductPatchRequest request
    ) {
        return ApiResponse.of("Book updated", productService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a book product")
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }
}
