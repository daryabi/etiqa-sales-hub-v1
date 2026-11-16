package com.etiqa.sales.product;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ProductImportService productImportService;

    @BeforeEach
    void setUp() {
        productImportService = new ProductImportService(
                productRepository, auditLogService, new TransactionTemplate(transactionManager), TestMessages.create());
    }

    @Test
    void importsGoodRowsAndReportsBadOnes() {
        ProductImportResponse importSummary = productImportService.importProducts(textFile(
                "bookTitle|bookPrice|bookStockQuantity",
                "Domain-Driven Design|54.99|15",
                "Invalid Column Count|10.00",
                "Negative Price|-5.00|10",
                "Refactoring|47.50|8"
        ));

        assertThat(importSummary.totalRowsProcessed()).isEqualTo(4);
        assertThat(importSummary.importedCount()).isEqualTo(2);
        assertThat(importSummary.failedCount()).isEqualTo(2);
        assertThat(importSummary.errors()).containsExactly(
                "Line 3: Expected 3 pipe-delimited columns (bookTitle|bookPrice|bookStockQuantity)",
                "Line 4: Price must be greater than 0 and stock cannot be negative."
        );
        verify(auditLogService).record(eq(AuditedResource.PRODUCT), eq(AuditAction.BULK_IMPORT), isNull(), eq(importSummary));
    }

    @Test
    void noHeader_firstRowIsData() {
        ArgumentCaptor<List<Product>> saved = savedProducts();

        ProductImportResponse importSummary = productImportService.importProducts(textFile(
                "﻿Clean Code|42.00|5",
                "",
                "   ",
                "  The Pragmatic Programmer  |  49.90 | 3 "
        ));

        assertThat(importSummary.totalRowsProcessed()).isEqualTo(2);
        assertThat(importSummary.failedCount()).isZero();
        verify(productRepository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(Product::getBookTitle)
                .containsExactly("Clean Code", "The Pragmatic Programmer");
    }

    @Test
    void commaDelimited_rejectsEveryRow() {
        ProductImportResponse importSummary = productImportService.importProducts(textFile(
                "bookTitle,bookPrice,bookStockQuantity",
                "Clean Code,42.00,5"
        ));

        assertThat(importSummary.totalRowsProcessed()).isEqualTo(2);
        assertThat(importSummary.importedCount()).isZero();
        assertThat(importSummary.errors()).allMatch(error -> error.contains("Expected 3 pipe-delimited columns"));
        verify(productRepository, never()).saveAll(anyList());
    }

    @Test
    void reportsBadTitlesAndNumbersByLine() {
        ProductImportResponse importSummary = productImportService.importProducts(textFile(
                "bookTitle|bookPrice|bookStockQuantity",
                "|10.00|1",
                "x".repeat(Product.TITLE_MAX_LENGTH + 1) + "|10.00|1",
                "Free Book|0.00|1",
                "Negative Stock|10.00|-1",
                "Bad Price|ten|1",
                "Bad Stock|10.00|1.5"
        ));

        assertThat(importSummary.importedCount()).isZero();
        assertThat(importSummary.errors()).containsExactly(
                "Line 2: Book title must be between 1 and 255 characters.",
                "Line 3: Book title must be between 1 and 255 characters.",
                "Line 4: Price must be greater than 0 and stock cannot be negative.",
                "Line 5: Price must be greater than 0 and stock cannot be negative.",
                "Line 6: Invalid numeric format for price or stock.",
                "Line 7: Invalid numeric format for price or stock."
        );
    }

    @Test
    void repeatedTitleInFile_isSkippedAsAlreadyInCatalog() {
        ArgumentCaptor<List<Product>> saved = savedProducts();

        ProductImportResponse importSummary = productImportService.importProducts(textFile(
                "Clean Code|42.00|5",
                "Refactoring|47.50|8",
                "clean code|40.00|2"
        ));

        assertThat(importSummary.importedCount()).isEqualTo(2);
        assertThat(importSummary.errors()).containsExactly("Line 3: 'clean code' is already in catalog.");
        verify(productRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Product::getBookTitle).containsExactly("Clean Code", "Refactoring");
    }

    @Test
    void titleAlreadyInDatabase_isSkippedAsAlreadyInCatalog() {
        when(productRepository.findExistingTitles(anyCollection())).thenReturn(List.of("refactoring"));

        ProductImportResponse importSummary = productImportService.importProducts(textFile(
                "bookTitle|bookPrice|bookStockQuantity",
                "Refactoring|47.50|8",
                "Bad Price|ten|1",
                "Clean Code|42.00|5"
        ));

        assertThat(importSummary.totalRowsProcessed()).isEqualTo(3);
        assertThat(importSummary.importedCount()).isEqualTo(1);
        assertThat(importSummary.errors()).containsExactly(
                "Line 2: 'Refactoring' is already in catalog.",
                "Line 3: Invalid numeric format for price or stock.");
    }

    @Test
    void nothingValid_skipsTheTitleLookup() {
        productImportService.importProducts(textFile("bookTitle|bookPrice|bookStockQuantity", "Bad|x|1"));

        verify(productRepository, never()).findExistingTitles(anyCollection());
    }

    @Test
    void headerOnly_importsNothing() {
        ProductImportResponse importSummary = productImportService.importProducts(textFile("bookTitle|bookPrice|bookStockQuantity"));

        assertThat(importSummary.totalRowsProcessed()).isZero();
        verify(productRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsMissingOrEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "books.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> productImportService.importProducts(empty))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Uploaded file must not be empty.");
        assertThatThrownBy(() -> productImportService.importProducts(null))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void unreadableFile_becomesBusinessError() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getInputStream()).thenThrow(new IOException("disk unplugged"));

        assertThatThrownBy(() -> productImportService.importProducts(broken))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Unable to read uploaded file: disk unplugged")
                .hasCauseInstanceOf(IOException.class);
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<Product>> savedProducts() {
        return ArgumentCaptor.forClass(List.class);
    }

    private static MockMultipartFile textFile(String... lines) {
        return new MockMultipartFile(
                "file",
                "books.txt",
                "text/plain",
                String.join("\n", lines).getBytes(StandardCharsets.UTF_8)
        );
    }
}
