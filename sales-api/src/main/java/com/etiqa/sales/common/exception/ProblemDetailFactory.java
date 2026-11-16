package com.etiqa.sales.common.exception;

import com.etiqa.sales.common.config.AppProperties;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import com.etiqa.sales.common.web.RequestResponseLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProblemDetailFactory {

    private final AppProperties appProperties;
    private final Messages messages;

    public ProblemDetail create(ErrorType errorType) {
        MessageKey detailKey = Objects.requireNonNull(errorType.getDetailKey(), errorType + " requires an explicit detail");
        return create(errorType, messages.get(detailKey));
    }

    public ProblemDetail create(ErrorType errorType, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorType.getStatus(), detail);
        problem.setTitle(messages.get(errorType.getTitleKey()));
        problem.setType(URI.create(appProperties.errorTypeBaseUri() + errorType.getTypeName()));
        problem.setProperty("timestamp", Instant.now());
        String correlationId = MDC.get(RequestResponseLoggingFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId != null) {
            problem.setProperty(RequestResponseLoggingFilter.CORRELATION_ID_MDC_KEY, correlationId);
        }
        return problem;
    }
}
