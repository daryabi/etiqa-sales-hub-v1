package com.etiqa.sales.sale;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import com.etiqa.sales.common.search.SearchPatterns;
import com.etiqa.sales.common.web.PageResponse;
import com.etiqa.sales.customer.Customer;
import com.etiqa.sales.customer.CustomerService;
import com.etiqa.sales.product.Product;
import com.etiqa.sales.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleRelationsLoader saleRelationsLoader;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    @Transactional
    public SaleResponse recordSale(SaleRequest request) {
        Customer customer = customerService.getCustomerOrThrow(request.customerId());
        Product product = productService.getProductOrThrow(request.bookId());

        checkProductIsActive(product);
        productService.deductStock(product, request.salesQuantity());

        Sale savedSale = saleRepository.save(Sale.builder()
                .customerId(customer.getCustomerId())
                .bookId(product.getBookId())
                .salesQuantity(request.salesQuantity())
                .salesPrice(product.getBookPrice())
                .build());

        SaleResponse response = toResponse(savedSale, product, customer);
        auditLogService.record(AuditedResource.SALE, AuditAction.CREATE, response.saleId(), response);
        eventPublisher.publishEvent(new SaleRecordedEvent(
                response.saleId(), response.bookId(), response.customerId(), response.salesQuantity(), response.totalAmount()));
        return response;
    }

    @Transactional
    public SaleResponse replaceSale(Long saleId, SaleRequest request) {
        Sale sale = getSaleOrThrow(saleId);
        return applySaleChanges(sale, request.customerId(), request.bookId(), request.salesQuantity(), AuditAction.UPDATE);
    }

    @Transactional
    public SaleResponse patchSale(Long saleId, SalePatchRequest request) {
        Sale sale = getSaleOrThrow(saleId);
        Long customerId = request.customerId() != null ? request.customerId() : sale.getCustomerId();
        Long bookId = request.bookId() != null ? request.bookId() : sale.getBookId();
        Integer quantity = request.salesQuantity() != null ? request.salesQuantity() : sale.getSalesQuantity();
        return applySaleChanges(sale, customerId, bookId, quantity, AuditAction.PATCH);
    }

    private SaleResponse applySaleChanges(
            Sale sale,
            Long customerId,
            Long bookId,
            Integer newQuantity,
            AuditAction auditAction
    ) {
        Customer customer = customerService.getCustomerOrThrow(customerId);
        Product originalBook = productService.getProductOrThrow(sale.getBookId());
        boolean bookChanged = !sale.getBookId().equals(bookId);
        Product newBook = bookChanged ? productService.getProductOrThrow(bookId) : originalBook;

        if (bookChanged || newQuantity > sale.getSalesQuantity()) {
            checkProductIsActive(newBook);
        }
        if (bookChanged) {
            productService.returnStock(originalBook, sale.getSalesQuantity());
            productService.deductStock(newBook, newQuantity);
            sale.setSalesPrice(newBook.getBookPrice());
        } else if (newQuantity > sale.getSalesQuantity()) {
            productService.deductStock(newBook, newQuantity - sale.getSalesQuantity());
        } else if (newQuantity < sale.getSalesQuantity()) {
            productService.returnStock(newBook, sale.getSalesQuantity() - newQuantity);
        }

        sale.setCustomerId(customer.getCustomerId());
        sale.setBookId(newBook.getBookId());
        sale.setSalesQuantity(newQuantity);

        Sale savedSale = saleRepository.save(sale);
        SaleResponse response = toResponse(savedSale, newBook, customer);
        auditLogService.record(AuditedResource.SALE, auditAction, response.saleId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(Long saleId) {
        Sale sale = getSaleOrThrow(saleId);
        Product product = productService.getProductOrThrow(sale.getBookId());
        Customer customer = customerService.getCustomerOrThrow(sale.getCustomerId());
        return toResponse(sale, product, customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> getSales(String search, Pageable pageable) {
        Page<Sale> page = SearchPatterns.toContainsLikePattern(search)
                .map(likePattern -> saleRepository.search(likePattern, pageable))
                .orElseGet(() -> saleRepository.findAll(pageable));
        return PageResponse.from(page, toResponses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findReportLines(Instant from, Instant toExclusive) {
        return toResponses(saleRepository.findSalesInPeriod(from, toExclusive));
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse summarize() {
        return saleRepository.summarize();
    }

    private List<SaleResponse> toResponses(List<Sale> sales) {
        SaleRelations relations = saleRelationsLoader.loadFor(sales);
        return sales.stream()
                .map(sale -> toResponse(sale, relations.bookOf(sale), relations.customerOf(sale)))
                .toList();
    }

    private void checkProductIsActive(Product product) {
        if (!product.isActive()) {
            throw new BusinessValidationException(messages.get(MessageKey.SALE_PRODUCT_INACTIVE, product.getBookId()));
        }
    }

    @Transactional
    public void deleteSale(Long saleId) {
        Sale sale = getSaleOrThrow(saleId);
        productService.returnStock(productService.getProductOrThrow(sale.getBookId()), sale.getSalesQuantity());
        saleRepository.delete(sale);
        auditLogService.record(AuditedResource.SALE, AuditAction.DELETE, saleId, null);
    }

    private Sale getSaleOrThrow(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get(MessageKey.SALE_NOT_FOUND, saleId)));
    }

    private SaleResponse toResponse(Sale sale, Product product, Customer customer) {
        BigDecimal totalAmount = sale.getSalesPrice().multiply(BigDecimal.valueOf(sale.getSalesQuantity()));
        return new SaleResponse(
                sale.getSaleId(),
                sale.getSaleDate(),
                sale.getBookId(),
                product.getBookTitle(),
                sale.getCustomerId(),
                customer.getFirstName() + " " + customer.getLastName(),
                sale.getSalesQuantity(),
                sale.getSalesPrice(),
                totalAmount
        );
    }
}
