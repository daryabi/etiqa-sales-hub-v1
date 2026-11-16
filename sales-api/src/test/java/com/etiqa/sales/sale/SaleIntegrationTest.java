package com.etiqa.sales.sale;

import com.etiqa.sales.audit.AuditLogResponse;
import com.etiqa.sales.common.config.KafkaConfig;
import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.common.web.PageResponse;
import com.etiqa.sales.customer.CustomerRequest;
import com.etiqa.sales.customer.CustomerResponse;
import com.etiqa.sales.product.InventorySummaryResponse;
import com.etiqa.sales.product.ProductImportResponse;
import com.etiqa.sales.product.ProductPatchRequest;
import com.etiqa.sales.product.ProductRequest;
import com.etiqa.sales.product.ProductResponse;
import com.etiqa.sales.product.RestockRequest;
import com.etiqa.sales.support.ApiIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SaleIntegrationTest extends ApiIntegrationTest {

    @Test
    void fullSaleFlow_endsInPdfReport() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        Consumer<HttpHeaders> admin = bearer(login("admin", "admin123"));

        Reply<ApiResponse<CustomerResponse>> customerRes = call(HttpMethod.POST, "/api/v1/customers", staff,
                new CustomerRequest("Alice", "Tan", "alice.tan@etiqa.com.my", "alice@example.com"),
                envelope(CustomerResponse.class));
        assertThat(customerRes.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(customerRes.body().message()).isEqualTo("Customer created");
        CustomerResponse createdCustomer = customerRes.body().data();

        PageResponse<CustomerResponse> customerSearch = data(call(HttpMethod.GET, "/api/v1/customers?search=alice&size=5",
                staff, null, pageEnvelope(CustomerResponse.class)));
        assertThat(customerSearch.content())
                .extracting(CustomerResponse::customerId)
                .contains(createdCustomer.customerId());

        ProductImportResponse imported = data(uploadBooks(staff,
                "bookTitle|bookPrice|bookStockQuantity\nEffective Java|45.50|12\nClean Architecture|39.00|20\n"));
        assertThat(imported.importedCount()).isEqualTo(2);

        PageResponse<ProductResponse> firstCatalogRead = getProducts("/api/v1/products?search=effective java");
        PageResponse<ProductResponse> cachedCatalogRead = getProducts("/api/v1/products?search=effective java");
        assertThat(cachedCatalogRead).isEqualTo(firstCatalogRead);
        ProductResponse effectiveJava = cachedCatalogRead.content().stream()
                .filter(product -> "Effective Java".equals(product.bookTitle()))
                .findFirst()
                .orElseThrow();

        PageResponse<ProductResponse> pageOfOne = getProducts("/api/v1/products?size=1&page=0");
        assertThat(pageOfOne.content()).hasSize(1);
        assertThat(pageOfOne.totalElements()).isGreaterThanOrEqualTo(2);

        ProductResponse restocked = data(call(HttpMethod.POST, "/api/v1/products/" + effectiveJava.bookId() + "/restock",
                staff, new RestockRequest(3), envelope(ProductResponse.class)));
        assertThat(restocked.bookStockQuantity()).isEqualTo(15);

        InventorySummaryResponse inventory = data(call(HttpMethod.GET, "/api/v1/products/summary",
                noAuth(), null, envelope(InventorySummaryResponse.class)));
        assertThat(inventory.activeTitles()).isGreaterThanOrEqualTo(2);

        Reply<ApiResponse<SaleResponse>> saleRes = call(HttpMethod.POST, "/api/v1/sales", staff,
                new SaleRequest(createdCustomer.customerId(), effectiveJava.bookId(), 8), envelope(SaleResponse.class));
        assertThat(saleRes.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(saleRes.body().message()).isEqualTo("Sale recorded");
        SaleResponse recordedSale = saleRes.body().data();
        assertThat(recordedSale.totalAmount()).isEqualByComparingTo("364.00");

        SaleResponse patchedSale = data(call(HttpMethod.PATCH, "/api/v1/sales/" + recordedSale.saleId(), staff,
                new SalePatchRequest(null, null, 7), envelope(SaleResponse.class)));
        assertThat(patchedSale.salesQuantity()).isEqualTo(7);

        PageResponse<ProductResponse> afterPatch = getProducts("/api/v1/products?search=effective java");
        assertThat(afterPatch.content())
                .filteredOn(product -> product.bookId().equals(effectiveJava.bookId()))
                .singleElement()
                .extracting(ProductResponse::bookStockQuantity)
                .isEqualTo(8);

        Reply<ApiResponse<ProductResponse>> repriced = call(HttpMethod.PATCH, "/api/v1/products/" + effectiveJava.bookId(),
                admin, new ProductPatchRequest(null, new BigDecimal("99.99"), null, null), envelope(ProductResponse.class));
        assertThat(repriced.status()).isEqualTo(HttpStatus.OK);
        assertThat(repriced.body().message()).isEqualTo("Book updated");

        SaleResponse repatchedSale = data(call(HttpMethod.PATCH, "/api/v1/sales/" + recordedSale.saleId(), staff,
                new SalePatchRequest(null, null, 3), envelope(SaleResponse.class)));
        assertThat(repatchedSale.unitPrice()).isEqualByComparingTo("45.50");
        assertThat(repatchedSale.totalAmount()).isEqualByComparingTo("136.50");

        PageResponse<SaleResponse> salesSearch = data(call(HttpMethod.GET, "/api/v1/sales?search=effective", staff,
                null, pageEnvelope(SaleResponse.class)));
        assertThat(salesSearch.content())
                .extracting(SaleResponse::saleId)
                .contains(recordedSale.saleId());

        SalesSummaryResponse salesSummary = data(call(HttpMethod.GET, "/api/v1/sales/summary", staff,
                null, envelope(SalesSummaryResponse.class)));
        assertThat(salesSummary.orderCount()).isGreaterThanOrEqualTo(1);
        assertThat(salesSummary.revenue()).isGreaterThanOrEqualTo(new BigDecimal("136.50"));

        Reply<Object> deleteCustomerWithSales = call(HttpMethod.DELETE, "/api/v1/customers/" + createdCustomer.customerId(),
                admin, null, ANY_BODY);
        assertThat(deleteCustomerWithSales.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(deleteCustomerWithSales.problem().getTitle()).isEqualTo("Data Integrity Violation");

        Reply<Object> oversell = call(HttpMethod.POST, "/api/v1/sales", staff,
                new SaleRequest(createdCustomer.customerId(), effectiveJava.bookId(), 999), ANY_BODY);
        assertThat(oversell.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(oversell.problem().getTitle()).isEqualTo("Insufficient Stock");

        PageResponse<AuditLogResponse> auditPage = data(call(HttpMethod.GET, "/api/v1/audit-logs?entityName=SALE&size=5",
                admin, null, pageEnvelope(AuditLogResponse.class)));
        assertThat(auditPage.content()).isNotEmpty();
        assertThat(auditPage.content().get(0).performedBy()).isEqualTo("staff");

        String malaysiaToday = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur")).toString();
        Consumer<HttpHeaders> staffAcceptingAnything = staff.andThen(headers -> headers.setAccept(List.of(MediaType.ALL)));
        Reply<byte[]> pdf = call(HttpMethod.GET, "/api/v1/reports/sales/pdf?startDate=" + malaysiaToday + "&endDate=" + malaysiaToday,
                staffAcceptingAnything, null, BYTES);
        assertThat(pdf.status()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(pdf.headers().getContentDisposition().isAttachment()).isTrue();
        assertThat(pdf.headers().getContentDisposition().getFilename())
                .isEqualTo("sales-report-" + malaysiaToday + "-" + malaysiaToday + ".pdf");
        assertThat(pdf.headers().getContentLength()).isEqualTo(pdf.body().length);
        assertThat(pdf.body().length).isGreaterThan(1000);
        assertThat(new String(pdf.body(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        Reply<Object> missingParam = call(HttpMethod.GET, "/api/v1/reports/sales/pdf?startDate=" + malaysiaToday,
                staffAcceptingAnything, null, ANY_BODY);
        assertThat(missingParam.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingParam.problem().getTitle()).isEqualTo("Bad Request");

        Reply<Object> deleteProduct = call(HttpMethod.DELETE, "/api/v1/products/" + effectiveJava.bookId(), admin, null, ANY_BODY);
        assertThat(deleteProduct.status()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getProducts("/api/v1/products?search=effective java").content())
                .extracting(ProductResponse::bookId)
                .doesNotContain(effectiveJava.bookId());
    }

    @Test
    void lastCopyRace_neverOversells() throws Exception {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        long customerId = createCustomer(staff, "race").customerId();
        long bookId = createBook(staff, "Last Copy Standing", "25.00", 1).bookId();
        int buyers = 8;

        CountDownLatch startingGun = new CountDownLatch(1);
        List<Future<HttpStatusCode>> attempts = new ArrayList<>();
        List<HttpStatusCode> outcomes = new ArrayList<>();
        try (ExecutorService buyerThreads = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int buyer = 0; buyer < buyers; buyer++) {
                attempts.add(buyerThreads.submit(() -> {
                    startingGun.await();
                    return call(HttpMethod.POST, "/api/v1/sales", staff,
                            new SaleRequest(customerId, bookId, 1), ANY_BODY).status();
                }));
            }
            startingGun.countDown();
            for (Future<HttpStatusCode> attempt : attempts) {
                outcomes.add(attempt.get(ASYNC_TIMEOUT.toSeconds(), TimeUnit.SECONDS));
            }
        }

        assertThat(outcomes).filteredOn(status -> status.equals(HttpStatus.CREATED)).hasSize(1);
        assertThat(outcomes).filteredOn(status -> status.equals(HttpStatus.CONFLICT)).hasSize(buyers - 1);
        assertThat(getBook(staff, bookId).bookStockQuantity()).isZero();
        assertThat(data(call(HttpMethod.GET, "/api/v1/sales?search=last copy standing", staff, null,
                pageEnvelope(SaleResponse.class))).totalElements()).isEqualTo(1);
    }

    @Test
    void lowStockSale_isAuditedViaKafka() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        long customerId = createCustomer(staff, "kafka").customerId();
        long bookId = createBook(staff, "Streaming Systems", "55.00", appProperties.lowStockThreshold() + 2).bookId();

        assertThat(call(HttpMethod.POST, "/api/v1/sales", staff, new SaleRequest(customerId, bookId, 5), ANY_BODY).status())
                .isEqualTo(HttpStatus.CREATED);

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> assertThat(auditLogRepository.findAll())
                .filteredOn(entry -> "LOW_STOCK_ALERT".equals(entry.getAction()) && Long.valueOf(bookId).equals(entry.getEntityId()))
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.getEntityName()).isEqualTo("PRODUCT");
                    assertThat(alert.getPerformedBy()).isEqualTo("SYSTEM");
                    assertThat(alert.getPayload()).contains("\"remainingStock\": 7");
                }));
    }

    @Test
    void badEvent_goesToDeadLetterTopic() {
        String deadLetterTopic = appProperties.topics().stock() + KafkaConfig.DEAD_LETTER_SUFFIX;
        String poisonKey = "poison-" + UUID.randomUUID();
        long auditEntriesBefore = auditLogRepository.count();

        kafkaTemplate.send(appProperties.topics().stock(), poisonKey, "not-json");

        try (var deadLetters = consumerFactory.createConsumer("dead-letter-verifier", null)) {
            deadLetters.subscribe(List.of(deadLetterTopic));
            ConsumerRecord<String, String> deadLetter = KafkaTestUtils.getSingleRecord(deadLetters, deadLetterTopic, ASYNC_TIMEOUT);
            assertThat(deadLetter.key()).isEqualTo(poisonKey);
            assertThat(deadLetter.value()).isEqualTo("not-json");
        }
        assertThat(auditLogRepository.count()).isEqualTo(auditEntriesBefore);
    }

    @Test
    void putReplacesBookAndSale() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        long customerId = createCustomer(staff, "replace").customerId();
        long bookId = createBook(staff, "Release It", "60.00", 10).bookId();

        Reply<ApiResponse<ProductResponse>> replacedBook = call(HttpMethod.PUT, "/api/v1/products/" + bookId, staff,
                new ProductRequest("Release It! 2nd Edition", new BigDecimal("65.00"), 10), envelope(ProductResponse.class));
        assertThat(replacedBook.body().message()).isEqualTo("Book updated");
        assertThat(getBook(staff, bookId).bookTitle()).isEqualTo("Release It! 2nd Edition");

        SaleResponse sale = data(call(HttpMethod.POST, "/api/v1/sales", staff,
                new SaleRequest(customerId, bookId, 3), envelope(SaleResponse.class)));
        Reply<ApiResponse<SaleResponse>> replacedSale = call(HttpMethod.PUT, "/api/v1/sales/" + sale.saleId(), staff,
                new SaleRequest(customerId, bookId, 1), envelope(SaleResponse.class));
        assertThat(replacedSale.body().message()).isEqualTo("Sale updated");

        SaleResponse reloaded = data(call(HttpMethod.GET, "/api/v1/sales/" + sale.saleId(), staff, null, envelope(SaleResponse.class)));
        assertThat(reloaded.salesQuantity()).isEqualTo(1);
        assertThat(reloaded.totalAmount()).isEqualByComparingTo("65.00");
        assertThat(getBook(staff, bookId).bookStockQuantity()).isEqualTo(9);
    }

    @Test
    void retryWithSameIdempotencyKey_recordsTheSaleOnce() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        long customerId = createCustomer(staff, "idempotent").customerId();
        long bookId = createBook(staff, "Designing Data-Intensive Applications", "120.00", 5).bookId();
        String idempotencyKey = UUID.randomUUID().toString();
        Consumer<HttpHeaders> withKey = staff.andThen(headers -> headers.set("Idempotency-Key", idempotencyKey));
        SaleRequest sale = new SaleRequest(customerId, bookId, 1);

        SaleResponse first = data(call(HttpMethod.POST, "/api/v1/sales", withKey, sale, envelope(SaleResponse.class)));
        SaleResponse retry = data(call(HttpMethod.POST, "/api/v1/sales", withKey, sale, envelope(SaleResponse.class)));
        SaleResponse separate = data(call(HttpMethod.POST, "/api/v1/sales", staff, sale, envelope(SaleResponse.class)));

        assertThat(retry.saleId()).isEqualTo(first.saleId());
        assertThat(separate.saleId()).isNotEqualTo(first.saleId());
        assertThat(getBook(staff, bookId).bookStockQuantity()).isEqualTo(3);
    }

    @Test
    void sameKeyWithDifferentSale_isRejectedAndOtherUsersAreUnaffected() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        Consumer<HttpHeaders> admin = bearer(login("admin", "admin123"));
        long customerId = createCustomer(staff, "key-reuse").customerId();
        long bookId = createBook(staff, "Accelerate", "70.00", 10).bookId();
        String sharedKey = UUID.randomUUID().toString();
        Consumer<HttpHeaders> staffWithKey = staff.andThen(headers -> headers.set("Idempotency-Key", sharedKey));
        Consumer<HttpHeaders> adminWithKey = admin.andThen(headers -> headers.set("Idempotency-Key", sharedKey));

        SaleResponse staffSale = data(call(HttpMethod.POST, "/api/v1/sales", staffWithKey,
                new SaleRequest(customerId, bookId, 1), envelope(SaleResponse.class)));
        Reply<Object> differentSale = call(HttpMethod.POST, "/api/v1/sales", staffWithKey,
                new SaleRequest(customerId, bookId, 3), ANY_BODY);
        SaleResponse adminSale = data(call(HttpMethod.POST, "/api/v1/sales", adminWithKey,
                new SaleRequest(customerId, bookId, 1), envelope(SaleResponse.class)));

        assertThat(differentSale.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(differentSale.problem().getTitle()).isEqualTo("Idempotency Key Reused");
        assertThat(adminSale.saleId()).isNotEqualTo(staffSale.saleId());
        assertThat(getBook(staff, bookId).bookStockQuantity()).isEqualTo(8);
    }

    @Test
    void deleteSale_isAdminOnlyAndGivesStockBack() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        Consumer<HttpHeaders> admin = bearer(login("admin", "admin123"));
        long customerId = createCustomer(staff, "refund").customerId();
        long bookId = createBook(staff, "Release It!", "60.00", 10).bookId();
        SaleResponse sale = data(call(HttpMethod.POST, "/api/v1/sales", staff,
                new SaleRequest(customerId, bookId, 3), envelope(SaleResponse.class)));

        Reply<Object> staffDelete = call(HttpMethod.DELETE, "/api/v1/sales/" + sale.saleId(), staff, null, ANY_BODY);
        Reply<Object> adminDelete = call(HttpMethod.DELETE, "/api/v1/sales/" + sale.saleId(), admin, null, ANY_BODY);
        Reply<Object> deletedAgain = call(HttpMethod.DELETE, "/api/v1/sales/" + sale.saleId(), admin, null, ANY_BODY);

        assertThat(staffDelete.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(adminDelete.status()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deletedAgain.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/api/v1/sales/" + sale.saleId(), staff, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getBook(staff, bookId).bookStockQuantity()).isEqualTo(10);
        assertThat(auditLogRepository.findAll()).anySatisfy(entry -> {
            assertThat(entry.getEntityName()).isEqualTo("SALE");
            assertThat(entry.getEntityId()).isEqualTo(sale.saleId());
            assertThat(entry.getAction()).isEqualTo("DELETE");
        });
    }
}
