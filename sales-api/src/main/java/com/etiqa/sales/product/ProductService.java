package com.etiqa.sales.product;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import com.etiqa.sales.common.search.SearchPatterns;
import com.etiqa.sales.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .bookTitle(request.bookTitle().trim())
                .bookPrice(request.bookPrice())
                .bookStockQuantity(request.bookStockQuantity())
                .build();

        ProductResponse response = ProductResponse.from(productRepository.save(product));
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.CREATE, response.bookId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long bookId) {
        return ProductResponse.from(getProductOrThrow(bookId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ProductCacheConfig.PRODUCTS_CACHE)
    public PageResponse<ProductResponse> getActiveProducts(String search, Pageable pageable) {
        Page<Product> page = SearchPatterns.trimmedSearchTerm(search)
                .map(bookTitle -> productRepository.findByActiveTrueAndBookTitleContainingIgnoreCase(bookTitle, pageable))
                .orElseGet(() -> productRepository.findByActiveTrue(pageable));
        return PageResponse.from(page, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse summarizeInventory() {
        return productRepository.summarizeActiveInventory();
    }

    @Transactional(readOnly = true)
    public List<Product> findAllByIds(Collection<Long> bookIds) {
        return productRepository.findAllById(bookIds);
    }

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductResponse update(Long bookId, ProductRequest request) {
        Product product = getProductOrThrow(bookId);
        int stockBefore = product.getBookStockQuantity();
        product.setBookTitle(request.bookTitle().trim());
        product.setBookPrice(request.bookPrice());
        product.setBookStockQuantity(request.bookStockQuantity());

        ProductResponse response = ProductResponse.from(productRepository.save(product));
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.UPDATE, response.bookId(), response);
        publishStockChange(product, stockBefore);
        return response;
    }

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductResponse patch(Long bookId, ProductPatchRequest request) {
        Product product = getProductOrThrow(bookId);
        int stockBefore = product.getBookStockQuantity();
        if (request.bookTitle() != null) {
            product.setBookTitle(request.bookTitle().trim());
        }
        if (request.bookPrice() != null) {
            product.setBookPrice(request.bookPrice());
        }
        if (request.bookStockQuantity() != null) {
            product.setBookStockQuantity(request.bookStockQuantity());
        }
        if (request.isActive() != null) {
            product.setActive(request.isActive());
        }

        ProductResponse response = ProductResponse.from(productRepository.save(product));
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.PATCH, response.bookId(), response);
        publishStockChange(product, stockBefore);
        return response;
    }

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductResponse restock(Long bookId, int quantity) {
        if (productRepository.increaseStock(bookId, quantity, Instant.now()) == 0) {
            throw new ResourceNotFoundException(messages.get(MessageKey.PRODUCT_NOT_FOUND, bookId));
        }
        Product product = getProductOrThrow(bookId);
        ProductResponse response = ProductResponse.from(product);
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.RESTOCK, bookId, response);
        publishStockChange(product, product.getBookStockQuantity() - quantity);
        return response;
    }

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public void delete(Long bookId) {
        Product product = getProductOrThrow(bookId);
        product.setActive(false);
        productRepository.save(product);
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.DELETE, bookId, null);
    }

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public void deductStock(Product book, int quantity) {
        int stockBefore = book.getBookStockQuantity();
        if (stockBefore < quantity) {
            throw new InsufficientStockException(messages.get(MessageKey.SALE_STOCK_INSUFFICIENT,
                    book.getBookTitle(), stockBefore, quantity));
        }
        book.setBookStockQuantity(stockBefore - quantity);
        // Flushing here, not at commit, makes MySQL take this book's row lock before the sale insert.
        // Without it two checkouts of the same book can deadlock (the race test caught this).
        productRepository.saveAndFlush(book);
        publishStockChange(book, stockBefore);
    }

    @Transactional
    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public void returnStock(Product book, int quantity) {
        int stockBefore = book.getBookStockQuantity();
        book.setBookStockQuantity(stockBefore + quantity);
        productRepository.saveAndFlush(book);
        publishStockChange(book, stockBefore);
    }

    @Transactional(readOnly = true)
    public Product getProductOrThrow(Long bookId) {
        return productRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get(MessageKey.PRODUCT_NOT_FOUND, bookId)));
    }

    private void publishStockChange(Product book, int stockBefore) {
        if (stockBefore != book.getBookStockQuantity()) {
            eventPublisher.publishEvent(new StockLevelChanged(
                    book.getBookId(), book.getBookTitle(), stockBefore, book.getBookStockQuantity()));
        }
    }
}
