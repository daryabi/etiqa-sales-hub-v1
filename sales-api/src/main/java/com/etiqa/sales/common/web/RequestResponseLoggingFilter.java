package com.etiqa.sales.common.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private static final int MAX_LOGGED_BODY_BYTES = 16_384;
    private static final String MASK = "***";
    private static final Set<String> MASKED_HEADERS = Set.of("authorization", "cookie", "x-api-key");
    private static final List<String> SENSITIVE_FIELD_HINTS = List.of("password", "token", "secret", "email");

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return requestPath != null && (requestPath.startsWith("/swagger-ui")
                || requestPath.startsWith("/v3/api-docs")
                || requestPath.startsWith("/actuator"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String requestTarget = requestTarget(request);
        log.info(
                "REQUEST {} {} correlationId={} headers={}",
                request.getMethod(),
                requestTarget,
                correlationId,
                formatHeadersForLog(request)
        );

        var wrappedRequest = new ContentCachingRequestWrapper(request, MAX_LOGGED_BODY_BYTES);
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        long startedAtMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            log.info(
                    "RESPONSE {} {} -> {} ({} ms) correlationId={} requestBody={} responseBody={}",
                    request.getMethod(),
                    requestTarget,
                    wrappedResponse.getStatus(),
                    System.currentTimeMillis() - startedAtMs,
                    correlationId,
                    formatBodyForLog(wrappedRequest.getContentAsByteArray(), request.getContentType(), request.getContentLengthLong()),
                    formatBodyForLog(wrappedResponse.getContentAsByteArray(), wrappedResponse.getContentType(), -1)
            );

            wrappedResponse.copyBodyToResponse();
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String requestTarget(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString == null ? request.getRequestURI() : request.getRequestURI() + "?" + queryString;
    }

    private String formatHeadersForLog(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>();
        for (String headerName : Collections.list(request.getHeaderNames())) {
            String lowerCaseName = headerName.toLowerCase(Locale.ROOT);
            headers.put(lowerCaseName, MASKED_HEADERS.contains(lowerCaseName) ? MASK : request.getHeader(headerName));
        }
        return headers.toString();
    }

    private String formatBodyForLog(byte[] bodyBytes, String contentType, long declaredLength) {
        if (contentType != null && contentType.startsWith(MediaType.MULTIPART_FORM_DATA.getType())) {
            return "<multipart: " + (declaredLength >= 0 ? declaredLength : bodyBytes.length) + " bytes>";
        }
        if (bodyBytes.length == 0) {
            return "<empty>";
        }
        if (contentType != null && (contentType.contains(MediaType.APPLICATION_PDF_VALUE)
                || contentType.contains(MediaType.APPLICATION_OCTET_STREAM_VALUE))) {
            return "<binary: " + bodyBytes.length + " bytes>";
        }
        String bodyText = new String(bodyBytes, StandardCharsets.UTF_8).trim();
        if (bodyText.startsWith("%PDF-")) {
            return "<PDF binary: " + bodyBytes.length + " bytes>";
        }
        if (bodyText.startsWith("{") || bodyText.startsWith("[")) {
            return maskJson(bodyText);
        }
        return bodyText.replace('\n', ' ').replace('\r', ' ');
    }

    private String maskJson(String json) {
        try {
            JsonNode tree = objectMapper.readTree(json);
            maskSensitiveFields(tree);
            return objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException exception) {
            return "<unreadable JSON: " + json.length() + " chars>";
        }
    }

    private void maskSensitiveFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> field : objectNode.properties()) {
                if (isSensitive(field.getKey()) && !field.getValue().isNull()) {
                    field.setValue(TextNode.valueOf(MASK));
                } else {
                    maskSensitiveFields(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::maskSensitiveFields);
        }
    }

    private boolean isSensitive(String fieldName) {
        String lowerCaseName = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_HINTS.stream().anyMatch(lowerCaseName::contains);
    }
}
