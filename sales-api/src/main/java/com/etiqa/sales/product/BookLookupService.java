package com.etiqa.sales.product;

import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.ExternalServiceException;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BookLookupService {

    static final String USER_AGENT = "etiqa-sales-hub";

    private static final Pattern ISBN_FORMAT = Pattern.compile("\\d{9}[\\dX]|\\d{13}");

    private final RestClient openLibraryRestClient;
    private final Cache lookupCache;
    private final Messages messages;

    public BookLookupService(RestClient openLibraryRestClient, CacheManager cacheManager, Messages messages) {
        this.openLibraryRestClient = openLibraryRestClient;
        this.lookupCache = Objects.requireNonNull(cacheManager.getCache(ProductCacheConfig.BOOK_LOOKUP_CACHE));
        this.messages = messages;
    }

    public BookLookupResponse lookUpByIsbn(String rawIsbn) {
        String isbn = rawIsbn.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (!ISBN_FORMAT.matcher(isbn).matches() || !hasValidCheckDigit(isbn)) {
            throw new BusinessValidationException(messages.get(MessageKey.BOOK_LOOKUP_ISBN_INVALID));
        }

        Optional<BookLookupResponse> cached = cachedLookup(isbn);
        if (cached.isPresent()) {
            return cached.get();
        }

        OpenLibraryBook book = fetchFromOpenLibrary(isbn)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get(MessageKey.BOOK_LOOKUP_NOT_FOUND, isbn)));
        BookLookupResponse response = new BookLookupResponse(
                isbn,
                book.title(),
                book.publishers() == null ? List.of() : List.copyOf(book.publishers()),
                book.publishDate(),
                book.numberOfPages()
        );
        rememberLookup(isbn, response);
        return response;
    }

    private Optional<BookLookupResponse> cachedLookup(String isbn) {
        try {
            return Optional.ofNullable(lookupCache.get(isbn, BookLookupResponse.class));
        } catch (RuntimeException exception) {
            log.warn("Book lookup cache unavailable, calling Open Library directly: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private void rememberLookup(String isbn, BookLookupResponse response) {
        try {
            lookupCache.put(isbn, response);
        } catch (RuntimeException exception) {
            log.warn("Could not cache book lookup for ISBN {}: {}", isbn, exception.getMessage());
        }
    }

    static boolean hasValidCheckDigit(String isbn) {
        if (isbn.length() == 10) {
            int sum = 0;
            for (int position = 0; position < 10; position++) {
                char character = isbn.charAt(position);
                int value = character == 'X' ? 10 : character - '0';
                sum += value * (10 - position);
            }
            return sum % 11 == 0;
        }
        if (isbn.indexOf('X') >= 0) {
            return false;
        }
        int sum = 0;
        for (int position = 0; position < 13; position++) {
            int digit = isbn.charAt(position) - '0';
            sum += position % 2 == 0 ? digit : digit * 3;
        }
        return sum % 10 == 0;
    }

    private Optional<OpenLibraryBook> fetchFromOpenLibrary(String isbn) {
        try {
            return openLibraryRestClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                            return Optional.empty();
                        }
                        if (response.getStatusCode().isError()) {
                            log.warn("Open Library answered {} for ISBN {}", response.getStatusCode().value(), isbn);
                            throw new ExternalServiceException(messages.get(MessageKey.BOOK_LOOKUP_UNAVAILABLE));
                        }
                        return Optional.ofNullable(response.bodyTo(OpenLibraryBook.class));
                    });
        } catch (ResourceAccessException exception) {
            log.warn("Open Library could not be reached for ISBN {}: {}", isbn, exception.getMessage());
            throw new ExternalServiceException(messages.get(MessageKey.BOOK_LOOKUP_UNAVAILABLE), exception);
        }
    }
}
