package com.etiqa.sales.product;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private static final int EXPECTED_COLUMNS = 3;
    private static final String COLUMN_SEPARATOR = "\\|";
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;
    private final Messages messages;

    @CacheEvict(value = ProductCacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductImportResponse importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException(messages.get(MessageKey.PRODUCT_IMPORT_EMPTY));
        }
        ParsedFile parsedFile = parse(file);
        return transactionTemplate.execute(status -> saveNewBooks(parsedFile));
    }

    private ParsedFile parse(MultipartFile file) {
        ParsedFile parsedFile = new ParsedFile();
        Set<String> titlesInFile = new HashSet<>();
        int lineNumber = 0;
        boolean headerChecked = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                String trimmedLine = line.replace(BYTE_ORDER_MARK, "").trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }
                List<String> columns = List.of(trimmedLine.split(COLUMN_SEPARATOR, -1));
                if (!headerChecked) {
                    headerChecked = true;
                    if (isHeader(columns)) {
                        continue;
                    }
                }
                parsedFile.processedRows++;
                Product book = parseLine(lineNumber, columns, parsedFile.errorsByLine);
                if (book == null) {
                    continue;
                }
                if (titlesInFile.add(normalizedTitle(book))) {
                    parsedFile.booksByLine.put(lineNumber, book);
                } else {
                    parsedFile.errorsByLine.put(lineNumber, alreadyInCatalog(lineNumber, book));
                }
            }
        } catch (IOException exception) {
            throw new BusinessValidationException(messages.get(MessageKey.PRODUCT_IMPORT_READ_ERROR, exception.getMessage()), exception);
        }
        return parsedFile;
    }

    private ProductImportResponse saveNewBooks(ParsedFile parsedFile) {
        Set<String> existingTitles = parsedFile.booksByLine.isEmpty()
                ? Set.of()
                : Set.copyOf(productRepository.findExistingTitles(
                        parsedFile.booksByLine.values().stream().map(this::normalizedTitle).toList()));

        List<Product> newBooks = new ArrayList<>();
        parsedFile.booksByLine.forEach((lineNumber, book) -> {
            if (existingTitles.contains(normalizedTitle(book))) {
                parsedFile.errorsByLine.put(lineNumber, alreadyInCatalog(lineNumber, book));
            } else {
                newBooks.add(book);
            }
        });
        if (!newBooks.isEmpty()) {
            productRepository.saveAll(newBooks);
        }

        List<String> errors = List.copyOf(parsedFile.errorsByLine.values());
        log.info("Imported {} of {} product rows ({} skipped)", newBooks.size(), parsedFile.processedRows, errors.size());
        ProductImportResponse response = new ProductImportResponse(parsedFile.processedRows, newBooks.size(), errors.size(), errors);
        auditLogService.record(AuditedResource.PRODUCT, AuditAction.BULK_IMPORT, null, response);
        return response;
    }

    private boolean isHeader(List<String> columns) {
        return columns.size() >= 2 && columns.get(1).trim().toLowerCase(Locale.ROOT).contains("price");
    }

    private Product parseLine(int lineNumber, List<String> columns, Map<Integer, String> errorsByLine) {
        if (columns.size() != EXPECTED_COLUMNS) {
            errorsByLine.put(lineNumber, messages.get(MessageKey.PRODUCT_IMPORT_LINE_COLUMNS, lineNumber, EXPECTED_COLUMNS));
            return null;
        }

        String bookTitle = columns.get(0).trim();
        if (bookTitle.isEmpty() || bookTitle.length() > Product.TITLE_MAX_LENGTH) {
            errorsByLine.put(lineNumber, messages.get(MessageKey.PRODUCT_IMPORT_LINE_TITLE, lineNumber, Product.TITLE_MAX_LENGTH));
            return null;
        }

        try {
            BigDecimal bookPrice = new BigDecimal(columns.get(1).trim());
            int stockQuantity = Integer.parseInt(columns.get(2).trim());
            if (bookPrice.compareTo(new BigDecimal(Product.MIN_PRICE)) < 0 || stockQuantity < Product.MIN_STOCK) {
                errorsByLine.put(lineNumber, messages.get(MessageKey.PRODUCT_IMPORT_LINE_RANGE, lineNumber));
                return null;
            }
            return Product.builder()
                    .bookTitle(bookTitle)
                    .bookPrice(bookPrice)
                    .bookStockQuantity(stockQuantity)
                    .build();
        } catch (NumberFormatException exception) {
            errorsByLine.put(lineNumber, messages.get(MessageKey.PRODUCT_IMPORT_LINE_FORMAT, lineNumber));
            return null;
        }
    }

    private String normalizedTitle(Product book) {
        return book.getBookTitle().toLowerCase(Locale.ROOT);
    }

    private String alreadyInCatalog(int lineNumber, Product book) {
        return messages.get(MessageKey.PRODUCT_IMPORT_LINE_DUPLICATE, lineNumber, book.getBookTitle());
    }

    private static final class ParsedFile {
        private final Map<Integer, Product> booksByLine = new TreeMap<>();
        private final Map<Integer, String> errorsByLine = new TreeMap<>();
        private int processedRows;
    }
}
