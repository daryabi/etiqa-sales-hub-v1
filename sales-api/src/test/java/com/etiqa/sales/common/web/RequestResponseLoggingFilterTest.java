package com.etiqa.sales.common.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseLoggingFilterTest {

    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(new ObjectMapper());
    private final Logger filterLogger = (Logger) LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logged.start();
        filterLogger.addAppender(logged);
    }

    @AfterEach
    void releaseLogs() {
        filterLogger.detachAppender(logged);
    }

    @Test
    void requestLineComesFirstWithMaskedHeadersAndQuery() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sales");
        request.setQueryString("page=0&size=10");
        request.addHeader("Authorization", "Bearer secret.jwt.token");
        request.addHeader("Accept", "application/json");
        request.addHeader(RequestResponseLoggingFilter.CORRELATION_ID_HEADER, "trace-1");

        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(MediaType.APPLICATION_JSON_VALUE, "{}"));

        assertThat(logged.list).hasSize(2);
        assertThat(message(0))
                .isEqualTo("REQUEST GET /api/v1/sales?page=0&size=10 correlationId=trace-1 headers="
                        + "{accept=application/json, authorization=***, x-correlation-id=trace-1}");
        assertThat(message(1)).startsWith("RESPONSE GET /api/v1/sales?page=0&size=10 -> 200");
    }

    @Test
    void masksSecretsInBothBodiesAndKeepsCorrelationId() throws Exception {
        MockHttpServletRequest request = jsonRequest("POST", "/api/v1/auth/login", "{\"username\":\"staff\",\"password\":\"staff123\"}");
        request.addHeader(RequestResponseLoggingFilter.CORRELATION_ID_HEADER, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, respondingWith(MediaType.APPLICATION_JSON_VALUE, "{\"data\":{\"token\":\"abc.def\"}}"));

        assertThat(response.getHeader(RequestResponseLoggingFilter.CORRELATION_ID_HEADER)).isEqualTo("trace-123");
        assertThat(response.getContentAsString()).contains("abc.def");
        assertThat(responseLine())
                .contains("correlationId=trace-123")
                .contains("requestBody={\"username\":\"staff\",\"password\":\"***\"}")
                .contains("responseBody={\"data\":{\"token\":\"***\"}}")
                .doesNotContain("staff123")
                .doesNotContain("abc.def");
        assertThat(MDC.get(RequestResponseLoggingFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void masksEmailsNumbersAndEscapedQuotesButKeepsOtherFields() throws Exception {
        String body = "{\"firstName\":\"Alice\",\"emailOffice\":\"alice@etiqa.com.my\",\"emailPersonal\":null,"
                + "\"password\":\"pa\\\"ss\",\"apiToken\":12345,\"items\":[{\"secret\":\"s\"}]}";

        filter.doFilter(jsonRequest("POST", "/api/v1/customers", body), new MockHttpServletResponse(),
                respondingWith(MediaType.APPLICATION_JSON_VALUE, "{}"));

        assertThat(responseLine())
                .contains("\"firstName\":\"Alice\"")
                .contains("\"emailOffice\":\"***\"")
                .contains("\"emailPersonal\":null")
                .contains("\"password\":\"***\"")
                .contains("\"apiToken\":\"***\"")
                .contains("[{\"secret\":\"***\"}]")
                .doesNotContain("alice@etiqa.com.my")
                .doesNotContain("pa\\\"ss")
                .doesNotContain("12345");
    }

    @Test
    void summarisesMultipartUploadsInsteadOfLoggingTheFile() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/products/upload");
        request.setContentType("multipart/form-data; boundary=abc");
        request.setContent("--abc\r\nContent-Disposition: form-data; name=\"file\"\r\n\r\nSecret Title|1.00|1\r\n--abc--".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(MediaType.APPLICATION_JSON_VALUE, "{}"));

        assertThat(responseLine()).contains("requestBody=<multipart: ").doesNotContain("Secret Title");
    }

    @Test
    void unreadableJsonIsSummarisedNotLogged() throws Exception {
        filter.doFilter(jsonRequest("POST", "/api/v1/auth/login", "{\"password\":\"oops"), new MockHttpServletResponse(),
                respondingWith(MediaType.APPLICATION_JSON_VALUE, "{}"));

        assertThat(responseLine()).contains("requestBody=<unreadable JSON: ").doesNotContain("oops");
    }

    @Test
    void newCorrelationId_andBinaryBodySummary() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reports/sales/pdf");
        request.addHeader(RequestResponseLoggingFilter.CORRELATION_ID_HEADER, " ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, respondingWith(MediaType.APPLICATION_PDF_VALUE, "%PDF-1.7 binary"));

        assertThat(response.getHeader(RequestResponseLoggingFilter.CORRELATION_ID_HEADER)).hasSize(36);
        assertThat(responseLine()).contains("requestBody=<empty>").contains("responseBody=<binary: 15 bytes>");
    }

    @Test
    void spotsPdfBytesWithoutPdfContentType() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/reports/raw"), new MockHttpServletResponse(),
                respondingWith(MediaType.TEXT_PLAIN_VALUE, "%PDF-1.7 body"));

        assertThat(responseLine()).contains("responseBody=<PDF binary: 13 bytes>");
    }

    @Test
    void plainTextIsLoggedOnOneLine() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/notes"), new MockHttpServletResponse(),
                respondingWith(MediaType.TEXT_PLAIN_VALUE, "line one\nline two"));

        assertThat(responseLine()).contains("responseBody=line one line two");
    }

    @Test
    void summarisesOctetStream() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/files/1"), new MockHttpServletResponse(),
                respondingWith(MediaType.APPLICATION_OCTET_STREAM_VALUE, "0101"));

        assertThat(responseLine()).contains("responseBody=<binary: 4 bytes>");
    }

    @Test
    void skipsDocsAndActuator() throws Exception {
        for (String path : List.of("/swagger-ui/index.html", "/v3/api-docs", "/actuator/health")) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", path), response, new MockFilterChain());
            assertThat(response.getHeader(RequestResponseLoggingFilter.CORRELATION_ID_HEADER)).isNull();
        }
        assertThat(logged.list).isEmpty();
    }

    private String message(int index) {
        return logged.list.get(index).getFormattedMessage();
    }

    private String responseLine() {
        assertThat(logged.list).hasSize(2);
        return message(1);
    }

    private static MockHttpServletRequest jsonRequest(String method, String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static MockFilterChain respondingWith(String contentType, String body) {
        return new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
                servletRequest.getInputStream().readAllBytes();
                servletResponse.setContentType(contentType);
                servletResponse.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}
