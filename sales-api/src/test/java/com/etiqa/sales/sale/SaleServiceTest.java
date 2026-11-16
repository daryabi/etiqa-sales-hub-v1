package com.etiqa.sales.sale;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.common.web.PageResponse;
import com.etiqa.sales.customer.Customer;
import com.etiqa.sales.customer.CustomerService;
import com.etiqa.sales.product.InsufficientStockException;
import com.etiqa.sales.product.Product;
import com.etiqa.sales.product.ProductRepository;
import com.etiqa.sales.product.ProductService;
import com.etiqa.sales.product.StockLevelChanged;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    private static final long SALE_ID = 100L;
    private static final long CUSTOMER_ID = 1L;
    private static final long BOOK_ID = 10L;
    private static final long OTHER_BOOK_ID = 20L;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private SaleRelationsLoader saleRelationsLoader;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProductService productService;
    private SaleService saleService;
    private Customer alice;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, auditLogService, eventPublisher, TestMessages.create());
        saleService = new SaleService(
                saleRepository,
                productService,
                customerService,
                saleRelationsLoader,
                auditLogService,
                eventPublisher,
                TestMessages.create()
        );
        alice = Customer.builder().customerId(CUSTOMER_ID).firstName("Alice").lastName("Tan").build();
    }

    @Test
    void recordSale_deductsStockAndPublishesEvent() {
        Product book = book(BOOK_ID, "Effective Java", "45.50", 12);
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        saveSalesWithGeneratedId();

        SaleResponse response = saleService.recordSale(new SaleRequest(CUSTOMER_ID, BOOK_ID, 5));

        assertThat(book.getBookStockQuantity()).isEqualTo(7);
        verify(productRepository).saveAndFlush(book);
        assertThat(response.saleId()).isEqualTo(SALE_ID);
        assertThat(response.unitPrice()).isEqualByComparingTo("45.50");
        assertThat(response.totalAmount()).isEqualByComparingTo("227.50");
        assertThat(response.customerName()).isEqualTo("Alice Tan");
        verify(auditLogService).record(AuditedResource.SALE, AuditAction.CREATE, SALE_ID, response);
        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 12, 7));
        verify(eventPublisher).publishEvent(new SaleRecordedEvent(SALE_ID, BOOK_ID, CUSTOMER_ID, 5, new BigDecimal("227.50")));
    }

    @Test
    void recordSale_rejectsInactiveBook() {
        Product retired = book(BOOK_ID, "Retired Title", "10.00", 5);
        retired.setActive(false);
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> saleService.recordSale(new SaleRequest(CUSTOMER_ID, BOOK_ID, 1)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Cannot sell inactive product with ID: " + BOOK_ID);

        assertThat(retired.getBookStockQuantity()).isEqualTo(5);
        verifyNoInteractions(saleRepository, eventPublisher);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordSale_rejectsMoreThanInStock() {
        Product book = book(BOOK_ID, "Refactoring", "47.50", 2);
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> saleService.recordSale(new SaleRequest(CUSTOMER_ID, BOOK_ID, 3)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for book 'Refactoring'. Available: 2, Requested: 3");

        assertThat(book.getBookStockQuantity()).isEqualTo(2);
        verifyNoInteractions(saleRepository, eventPublisher);
    }

    @Test
    void replaceSale_lowerQuantityGivesStockBackAndKeepsOldPrice() {
        Sale sale = existingSale(BOOK_ID, 5, "45.50");
        Product book = book(BOOK_ID, "Effective Java", "99.99", 7);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(saleRepository.save(sale)).thenReturn(sale);

        SaleResponse response = saleService.replaceSale(SALE_ID, new SaleRequest(CUSTOMER_ID, BOOK_ID, 2));

        assertThat(book.getBookStockQuantity()).isEqualTo(10);
        assertThat(response.unitPrice()).isEqualByComparingTo("45.50");
        assertThat(response.totalAmount()).isEqualByComparingTo("91.00");
        verify(auditLogService).record(AuditedResource.SALE, AuditAction.UPDATE, SALE_ID, response);
        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 7, 10));
        verify(eventPublisher, never()).publishEvent(any(SaleRecordedEvent.class));
    }

    @Test
    void patchSale_higherQuantityTakesOnlyTheDifference() {
        Sale sale = existingSale(BOOK_ID, 2, "45.50");
        Product book = book(BOOK_ID, "Effective Java", "45.50", 4);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(saleRepository.save(sale)).thenReturn(sale);

        saleService.patchSale(SALE_ID, new SalePatchRequest(null, null, 6));

        assertThat(book.getBookStockQuantity()).isZero();
        assertThat(sale.getSalesQuantity()).isEqualTo(6);
        verify(saleRepository, times(1)).findById(SALE_ID);
        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 4, 0));
    }

    @Test
    void patchSale_rejectsQuantityAboveWhatIsLeft() {
        Sale sale = existingSale(BOOK_ID, 2, "45.50");
        Product book = book(BOOK_ID, "Effective Java", "45.50", 1);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> saleService.patchSale(SALE_ID, new SalePatchRequest(null, null, 4)))
                .isInstanceOf(InsufficientStockException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void patchSale_switchingBookMovesStockAndUsesNewPrice() {
        Sale sale = existingSale(BOOK_ID, 3, "45.50");
        Product original = book(BOOK_ID, "Effective Java", "45.50", 9);
        Product replacement = book(OTHER_BOOK_ID, "Clean Architecture", "39.00", 20);
        Customer bob = Customer.builder().customerId(2L).firstName("Bob").lastName("Lee").build();
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerOrThrow(2L)).thenReturn(bob);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(original));
        when(productRepository.findById(OTHER_BOOK_ID)).thenReturn(Optional.of(replacement));
        when(saleRepository.save(sale)).thenReturn(sale);

        SaleResponse response = saleService.patchSale(SALE_ID, new SalePatchRequest(2L, OTHER_BOOK_ID, 4));

        assertThat(original.getBookStockQuantity()).isEqualTo(12);
        assertThat(replacement.getBookStockQuantity()).isEqualTo(16);
        assertThat(response.customerName()).isEqualTo("Bob Lee");
        assertThat(response.unitPrice()).isEqualByComparingTo("39.00");
        verify(productRepository).saveAndFlush(original);
        verify(productRepository).saveAndFlush(replacement);
        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 9, 12));
        verify(eventPublisher).publishEvent(new StockLevelChanged(OTHER_BOOK_ID, "Clean Architecture", 20, 16));
    }

    @Test
    void replaceSale_rejectsSwitchToInactiveBook() {
        Sale sale = existingSale(BOOK_ID, 1, "45.50");
        Product original = book(BOOK_ID, "Effective Java", "45.50", 9);
        Product retired = book(OTHER_BOOK_ID, "Retired Title", "10.00", 50);
        retired.setActive(false);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(original));
        when(productRepository.findById(OTHER_BOOK_ID)).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> saleService.replaceSale(SALE_ID, new SaleRequest(CUSTOMER_ID, OTHER_BOOK_ID, 1)))
                .isInstanceOf(BusinessValidationException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void patchSale_canStillReduceInactiveBook() {
        Sale sale = existingSale(BOOK_ID, 4, "45.50");
        Product retired = book(BOOK_ID, "Retired Title", "45.50", 0);
        retired.setActive(false);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(retired));
        when(saleRepository.save(sale)).thenReturn(sale);

        saleService.patchSale(SALE_ID, new SalePatchRequest(null, null, 1));

        assertThat(retired.getBookStockQuantity()).isEqualTo(3);
    }

    @Test
    void deleteSale_putsQuantityBackAndAudits() {
        Sale sale = existingSale(BOOK_ID, 4, "45.50");
        Product book = book(BOOK_ID, "Effective Java", "45.50", 6);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        saleService.deleteSale(SALE_ID);

        assertThat(book.getBookStockQuantity()).isEqualTo(10);
        verify(saleRepository).delete(sale);
        verify(auditLogService).record(AuditedResource.SALE, AuditAction.DELETE, SALE_ID, null);
        verify(eventPublisher).publishEvent(new StockLevelChanged(BOOK_ID, "Effective Java", 6, 10));
    }

    @Test
    void deleteSale_unknownSaleIsNotFound() {
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.deleteSale(SALE_ID)).isInstanceOf(ResourceNotFoundException.class);
        verify(saleRepository, never()).delete(any(Sale.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getById_unknownSaleIsNotFound() {
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.getById(SALE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sale not found with ID: " + SALE_ID);
    }

    @Test
    void getById_combinesSaleBookAndCustomer() {
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(existingSale(BOOK_ID, 2, "45.50")));
        when(productRepository.findById(BOOK_ID)).thenReturn(Optional.of(book(BOOK_ID, "Effective Java", "45.50", 3)));
        when(customerService.getCustomerOrThrow(CUSTOMER_ID)).thenReturn(alice);

        SaleResponse response = saleService.getById(SALE_ID);

        assertThat(response.bookTitle()).isEqualTo("Effective Java");
        assertThat(response.totalAmount()).isEqualByComparingTo("91.00");
    }

    @Test
    void getSales_searchUsesBatchLoadedBooksAndCustomers() {
        Pageable pageable = PageRequest.of(0, 20);
        Sale sale = existingSale(BOOK_ID, 1, "10.00");
        Page<Sale> page = new PageImpl<>(List.of(sale), pageable, 1);
        when(saleRepository.search(anyString(), eq(pageable))).thenReturn(page);
        when(saleRelationsLoader.loadFor(List.of(sale))).thenReturn(new SaleRelations(
                Map.of(BOOK_ID, book(BOOK_ID, "Effective Java", "10.00", 3)),
                Map.of(CUSTOMER_ID, alice)));

        PageResponse<SaleResponse> salesPage = saleService.getSales("effective", pageable);

        assertThat(salesPage.content()).singleElement().satisfies(found -> {
            assertThat(found.bookTitle()).isEqualTo("Effective Java");
            assertThat(found.customerName()).isEqualTo("Alice Tan");
        });
    }

    @Test
    void getSales_blankSearchListsEverything() {
        Pageable pageable = PageRequest.of(0, 20);
        when(saleRepository.findAll(pageable)).thenReturn(Page.empty(pageable));
        when(saleRelationsLoader.loadFor(List.of())).thenReturn(new SaleRelations(Map.of(), Map.of()));

        assertThat(saleService.getSales(" ", pageable).content()).isEmpty();
    }

    @Test
    void findReportLines_returnsSalesInPeriodWithNames() {
        Instant from = Instant.parse("2026-11-15T16:00:00Z");
        Instant to = Instant.parse("2026-11-16T16:00:00Z");
        Sale sale = existingSale(BOOK_ID, 2, "45.50");
        when(saleRepository.findSalesInPeriod(from, to)).thenReturn(List.of(sale));
        when(saleRelationsLoader.loadFor(List.of(sale))).thenReturn(new SaleRelations(
                Map.of(BOOK_ID, book(BOOK_ID, "Effective Java", "45.50", 3)),
                Map.of(CUSTOMER_ID, alice)));

        assertThat(saleService.findReportLines(from, to)).singleElement().satisfies(line -> {
            assertThat(line.bookTitle()).isEqualTo("Effective Java");
            assertThat(line.customerName()).isEqualTo("Alice Tan");
            assertThat(line.totalAmount()).isEqualByComparingTo("91.00");
        });
    }

    @Test
    void summarize_comesFromRepository() {
        SalesSummaryResponse summary = new SalesSummaryResponse(3L, 9L, new BigDecimal("120.00"));
        when(saleRepository.summarize()).thenReturn(summary);

        assertThat(saleService.summarize()).isEqualTo(summary);
    }

    private void saveSalesWithGeneratedId() {
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            sale.setSaleId(SALE_ID);
            sale.setSaleDate(Instant.parse("2026-11-15T09:00:00Z"));
            return sale;
        });
    }

    private static Product book(long id, String title, String price, int stock) {
        return Product.builder()
                .bookId(id)
                .bookTitle(title)
                .bookPrice(new BigDecimal(price))
                .bookStockQuantity(stock)
                .build();
    }

    private static Sale existingSale(long bookId, int quantity, String price) {
        return Sale.builder()
                .saleId(SALE_ID)
                .saleDate(Instant.parse("2026-11-14T09:00:00Z"))
                .customerId(CUSTOMER_ID)
                .bookId(bookId)
                .salesQuantity(quantity)
                .salesPrice(new BigDecimal(price))
                .build();
    }
}
