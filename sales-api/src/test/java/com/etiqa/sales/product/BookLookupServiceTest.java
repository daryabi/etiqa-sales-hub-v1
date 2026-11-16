package com.etiqa.sales.product;

import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.ExternalServiceException;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BookLookupServiceTest {

    private static final String OPEN_LIBRARY = "https://openlibrary.org";

    private MockRestServiceServer openLibrary;
    private BookLookupService bookLookupService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(OPEN_LIBRARY);
        openLibrary = MockRestServiceServer.bindTo(builder).build();
        bookLookupService = new BookLookupService(builder.build(),
                new ConcurrentMapCacheManager(ProductCacheConfig.BOOK_LOOKUP_CACHE), TestMessages.create());
    }

    @Test
    void lookUp_mapsOpenLibraryFields() {
        openLibrary.expect(requestTo(OPEN_LIBRARY + "/isbn/9780134685991.json"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, BookLookupService.USER_AGENT))
                .andRespond(withSuccess("""
                        {"title": "Effective Java", "publishers": ["Addison-Wesley"],
                         "publish_date": "2018", "number_of_pages": 412, "covers": [1234]}
                        """, MediaType.APPLICATION_JSON));

        BookLookupResponse book = bookLookupService.lookUpByIsbn("978-0-13-468599-1");

        assertThat(book.isbn()).isEqualTo("9780134685991");
        assertThat(book.title()).isEqualTo("Effective Java");
        assertThat(book.publishers()).containsExactly("Addison-Wesley");
        assertThat(book.publishDate()).isEqualTo("2018");
        assertThat(book.numberOfPages()).isEqualTo(412);
        openLibrary.verify();
    }

    @Test
    void lookUp_handlesMissingOptionalFields() {
        openLibrary.expect(requestTo(OPEN_LIBRARY + "/isbn/080442957X.json"))
                .andRespond(withSuccess("{\"title\": \"Some Book\"}", MediaType.APPLICATION_JSON));

        BookLookupResponse book = bookLookupService.lookUpByIsbn("0-8044-2957-x");

        assertThat(book.publishers()).isEmpty();
        assertThat(book.numberOfPages()).isNull();
    }

    @Test
    void unknownIsbn_becomesNotFound() {
        openLibrary.expect(requestTo(OPEN_LIBRARY + "/isbn/9780000000002.json"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> bookLookupService.lookUpByIsbn("9780000000002"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No book found for ISBN 9780000000002.");
    }

    @Test
    void upstreamError_becomesExternalServiceFailure() {
        openLibrary.expect(requestTo(OPEN_LIBRARY + "/isbn/9780134685991.json"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> bookLookupService.lookUpByIsbn("9780134685991"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("The book lookup service is not responding right now. Please try again later.");
    }

    @Test
    void timeout_becomesExternalServiceFailure() {
        openLibrary.expect(requestTo(OPEN_LIBRARY + "/isbn/9780134685991.json"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThatThrownBy(() -> bookLookupService.lookUpByIsbn("9780134685991"))
                .isInstanceOf(ExternalServiceException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void secondLookup_comesFromTheCache() {
        openLibrary.expect(times(1), requestTo(OPEN_LIBRARY + "/isbn/9780134685991.json"))
                .andRespond(withSuccess("{\"title\": \"Effective Java\"}", MediaType.APPLICATION_JSON));

        bookLookupService.lookUpByIsbn("9780134685991");
        BookLookupResponse again = bookLookupService.lookUpByIsbn("978-0-13-468599-1");

        assertThat(again.title()).isEqualTo("Effective Java");
        openLibrary.verify();
    }

    @Test
    void wrongCheckDigit_isRejectedWithoutCallingOpenLibrary() {
        assertThatThrownBy(() -> bookLookupService.lookUpByIsbn("9780134685992"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("check digit");

        openLibrary.verify();
    }

    @Test
    void checkDigits_followIsbn10AndIsbn13Rules() {
        assertThat(BookLookupService.hasValidCheckDigit("0134685997")).isTrue();
        assertThat(BookLookupService.hasValidCheckDigit("080442957X")).isTrue();
        assertThat(BookLookupService.hasValidCheckDigit("0134685998")).isFalse();
        assertThat(BookLookupService.hasValidCheckDigit("9780134685991")).isTrue();
        assertThat(BookLookupService.hasValidCheckDigit("978013468599X")).isFalse();
    }

    @Test
    void badIsbn_isRejectedWithoutCallingOpenLibrary() {
        assertThatThrownBy(() -> bookLookupService.lookUpByIsbn("12-34"))
                .isInstanceOf(BusinessValidationException.class);

        openLibrary.verify();
    }
}
