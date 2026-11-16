package com.etiqa.sales.product;

import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.sale.SaleRequest;
import com.etiqa.sales.sale.SaleResponse;
import com.etiqa.sales.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class ProductIntegrationTest extends ApiIntegrationTest {

    @Test
    void catalogCache_clearsOnEveryStockChange() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        long customerId = createCustomer(staff, "cache").customerId();
        long bookId = createBook(staff, "Cache Busting Patterns", "20.00", 30).bookId();
        String catalogUri = "/api/v1/products?search=busting";

        assertThat(getProducts(catalogUri).content()).singleElement()
                .satisfies(book -> assertThat(book.bookPrice()).isEqualByComparingTo("20.00"));
        assertThat(cachedCatalogPages()).isNotEmpty();

        data(call(HttpMethod.PATCH, "/api/v1/products/" + bookId, staff,
                new ProductPatchRequest(null, new BigDecimal("25.00"), null, null), envelope(ProductResponse.class)));
        assertThat(cachedCatalogPages()).isEmpty();
        assertThat(getProducts(catalogUri).content().get(0).bookPrice()).isEqualByComparingTo("25.00");
        assertThat(cachedCatalogPages()).isNotEmpty();

        data(call(HttpMethod.POST, "/api/v1/sales", staff, new SaleRequest(customerId, bookId, 4), envelope(SaleResponse.class)));
        assertThat(cachedCatalogPages()).isEmpty();
        assertThat(getProducts(catalogUri).content().get(0).bookStockQuantity()).isEqualTo(26);

        data(call(HttpMethod.POST, "/api/v1/products/" + bookId + "/restock", staff,
                new RestockRequest(5), envelope(ProductResponse.class)));
        assertThat(cachedCatalogPages()).isEmpty();
        assertThat(getProducts(catalogUri).content().get(0).bookStockQuantity()).isEqualTo(31);
    }

    @Test
    void upload_handlesBadFiles() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));

        ProductImportResponse commaSeparated = data(uploadBooks(staff,
                "bookTitle,bookPrice,bookStockQuantity\nClean Code,42.00,5\n"));
        assertThat(commaSeparated.importedCount()).isZero();
        assertThat(commaSeparated.failedCount()).isEqualTo(2);
        assertThat(commaSeparated.errors()).allMatch(error -> error.contains("Expected 3 pipe-delimited columns"));

        ProductImportResponse mixed = data(uploadBooks(staff, String.join("\n",
                "bookTitle|bookPrice|bookStockQuantity",
                "|10.00|1",
                "Free Lunch|0.00|1",
                "Ten Dollars|ten|1",
                "Upload Edge Cases|12.00|4")));
        assertThat(mixed.totalRowsProcessed()).isEqualTo(4);
        assertThat(mixed.importedCount()).isEqualTo(1);
        assertThat(mixed.errors()).containsExactly(
                "Line 2: Book title must be between 1 and 255 characters.",
                "Line 3: Price must be greater than 0 and stock cannot be negative.",
                "Line 4: Invalid numeric format for price or stock.");
        assertThat(getProducts("/api/v1/products?search=upload edge").content()).hasSize(1);

        Reply<ApiResponse<ProductImportResponse>> emptyFile = uploadBooks(staff, "");
        assertThat(emptyFile.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(emptyFile.problem().getDetail()).isEqualTo("Uploaded file must not be empty.");

        MultiValueMap<String, Object> formWithoutFile = new LinkedMultiValueMap<>();
        formWithoutFile.add("note", "no file");
        Reply<Object> missingFile = call(HttpMethod.POST, "/api/v1/products/upload",
                staff.andThen(headers -> headers.setContentType(MediaType.MULTIPART_FORM_DATA)),
                formWithoutFile, ANY_BODY);
        assertThat(missingFile.status()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(uploadBooks(noAuth(), "Anonymous|1.00|1").status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void isbnLookup_needsLoginAndAValidIsbn() {
        assertThat(call(HttpMethod.GET, "/api/v1/products/isbn/9780134685991", noAuth(), null, ANY_BODY).status())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        Reply<Object> badIsbn = call(HttpMethod.GET, "/api/v1/products/isbn/123", bearer(login("staff", "staff123")), null, ANY_BODY);
        assertThat(badIsbn.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badIsbn.problem().getTitle()).isEqualTo("Business Rule Violation");
    }

    @Test
    void reImportingTheSameFile_skipsEveryRow() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        String pipeFile = "Working Effectively with Legacy Code|45.00|2\nThe Phoenix Project|38.00|4\n";

        ProductImportResponse firstImport = data(uploadBooks(staff, pipeFile));
        ProductImportResponse secondImport = data(uploadBooks(staff, pipeFile));

        assertThat(firstImport.importedCount()).isEqualTo(2);
        assertThat(secondImport.importedCount()).isZero();
        assertThat(secondImport.errors()).containsExactly(
                "Line 1: 'Working Effectively with Legacy Code' is already in catalog.",
                "Line 2: 'The Phoenix Project' is already in catalog.");
    }
}
