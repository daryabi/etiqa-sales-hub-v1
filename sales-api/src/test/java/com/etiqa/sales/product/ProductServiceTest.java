package com.etiqa.sales.product;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final long BOOK_ID = 10L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, auditLogService, eventPublisher, TestMessages.create());
    }

    @Test
    void create_trimsTitle() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setBookId(BOOK_ID);
            return product;
        });

        ProductResponse response = productService.create(new ProductRequest("  Refactoring ", new BigDecimal("47.50"), 8));

        assertThat(response.bookTitle()).isEqualTo("Refactoring");
        assertThat(response.isActive()).isTrue();
        verify(auditLogService).record(AuditedResource.PRODUCT, AuditAction.CREATE, BOOK_ID, response);
    }

    @Test
    void update_replacesTitlePriceAndStock() {
        Product book = existingBook();
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(productRepository.save(book)).thenReturn(book);

        ProductResponse response = productService.update(BOOK_ID, new ProductRequest(" DDD ", new BigDecimal("54.99"), 3));

        assertThat(response.bookTitle()).isEqualTo("DDD");
        assertThat(response.bookPrice()).isEqualByComparingTo("54.99");
        assertThat(response.bookStockQuantity()).isEqualTo(3);
    }

    @Test
    void patch_onlyTouchesSentFields() {
        Product book = existingBook();
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(productRepository.save(book)).thenReturn(book);

        ProductResponse response = productService.patch(BOOK_ID, new ProductPatchRequest(null, new BigDecimal("99.99"), null, null));

        assertThat(response.bookTitle()).isEqualTo("Effective Java");
        assertThat(response.bookPrice()).isEqualByComparingTo("99.99");
        assertThat(response.bookStockQuantity()).isEqualTo(12);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void patch_withAllFields() {
        Product book = existingBook();
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(productRepository.save(book)).thenReturn(book);

        ProductResponse response = productService.patch(BOOK_ID,
                new ProductPatchRequest(" Java ", new BigDecimal("10.00"), 1, false));

        assertThat(response.bookTitle()).isEqualTo("Java");
        assertThat(response.bookStockQuantity()).isEqualTo(1);
        assertThat(response.isActive()).isFalse();
        verify(auditLogService).record(AuditedResource.PRODUCT, AuditAction.PATCH, BOOK_ID, response);
    }

    @Test
    void restock_unknownBookIsNotFound() {
        when(productRepository.increaseStock(eq(BOOK_ID), eq(5), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> productService.restock(BOOK_ID, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product (book) not found with ID: " + BOOK_ID);

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void restock_returnsFreshStock() {
        Product book = existingBook();
        book.setBookStockQuantity(17);
        when(productRepository.increaseStock(eq(BOOK_ID), eq(5), any(Instant.class))).thenReturn(1);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        ProductResponse response = productService.restock(BOOK_ID, 5);

        assertThat(response.bookStockQuantity()).isEqualTo(17);
        verify(auditLogService).record(AuditedResource.PRODUCT, AuditAction.RESTOCK, BOOK_ID, response);
    }

    @Test
    void delete_onlyDeactivates() {
        Product book = existingBook();
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        productService.delete(BOOK_ID);

        assertThat(book.isActive()).isFalse();
        verify(productRepository).save(book);
        verify(productRepository, never()).delete(any());
        verify(auditLogService).record(AuditedResource.PRODUCT, AuditAction.DELETE, BOOK_ID, null);
    }

    @Test
    void activeCatalog_searchesTitleOnlyWhenGiven() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByActiveTrueAndBookTitleContainingIgnoreCase("java", pageable))
                .thenReturn(new PageImpl<>(List.of(existingBook()), pageable, 1));
        when(productRepository.findByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(productService.getActiveProducts("  java ", pageable).content()).hasSize(1);
        assertThat(productService.getActiveProducts("", pageable).content()).isEmpty();
    }

    @Test
    void deductStock_takesQuantityAndAnnouncesTheChange() {
        Product book = existingBook();

        productService.deductStock(book, 5);

        assertThat(book.getBookStockQuantity()).isEqualTo(7);
        verify(productRepository).saveAndFlush(book);
        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 12, 7));
    }

    @Test
    void deductStock_rejectsMoreThanInStock() {
        Product book = existingBook();

        assertThatThrownBy(() -> productService.deductStock(book, 13))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for book 'Effective Java'. Available: 12, Requested: 13");

        assertThat(book.getBookStockQuantity()).isEqualTo(12);
        verify(productRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void returnStock_putsQuantityBack() {
        Product book = existingBook();

        productService.returnStock(book, 3);

        assertThat(book.getBookStockQuantity()).isEqualTo(15);
        verify(productRepository).saveAndFlush(book);
    }

    @Test
    void restock_announcesStockBeforeAndAfter() {
        Product book = existingBook();
        book.setBookStockQuantity(17);
        when(productRepository.increaseStock(eq(BOOK_ID), eq(5), any(Instant.class))).thenReturn(1);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        productService.restock(BOOK_ID, 5);

        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 12, 17));
    }

    @Test
    void patch_withoutStockChange_announcesNothing() {
        Product book = existingBook();
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(productRepository.save(book)).thenReturn(book);

        productService.patch(BOOK_ID, new ProductPatchRequest(null, new BigDecimal("50.00"), null, null));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void findAllByIds_delegatesToRepository() {
        when(productRepository.findAllById(List.of(BOOK_ID))).thenReturn(List.of(existingBook()));

        assertThat(productService.findAllByIds(List.of(BOOK_ID))).hasSize(1);
    }

    @Test
    void getById_mapsBook() {
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(existingBook()));

        assertThat(productService.getById(BOOK_ID).bookTitle()).isEqualTo("Effective Java");
    }

    private static Product existingBook() {
        return Product.builder()
                .bookId(BOOK_ID)
                .bookTitle("Effective Java")
                .bookPrice(new BigDecimal("45.50"))
                .bookStockQuantity(12)
                .build();
    }
}
