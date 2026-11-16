package com.etiqa.sales.audit;

import com.etiqa.sales.common.web.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private static final String AUDIT_ID = "auditId";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(AuditedResource resource, AuditAction action, Long entityId, Object snapshot) {
        String payloadJson = null;
        if (snapshot != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(snapshot);
            } catch (JsonProcessingException exception) {
                log.warn("Failed to serialize audit payload for {}: {}", resource, exception.getMessage());
                payloadJson = "{\"error\":\"Unserializable payload\"}";
            }
        }

        String actor = resolveCurrentUsername();
        AuditLog entry = AuditLog.builder()
                .entityName(resource.name())
                .action(action.name())
                .entityId(entityId)
                .performedBy(actor)
                .payload(payloadJson)
                .build();

        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findAll(Optional<AuditedResource> resourceFilter, Pageable pageable) {
        Pageable newestFirstOnTies = withAuditIdTieBreaker(pageable);
        Page<AuditLog> page = resourceFilter
                .map(resource -> auditLogRepository.findByEntityName(resource.name(), newestFirstOnTies))
                .orElseGet(() -> auditLogRepository.findAll(newestFirstOnTies));

        return PageResponse.from(page, entry -> new AuditLogResponse(
                entry.getAuditId(),
                entry.getEntityName(),
                entry.getAction(),
                entry.getEntityId(),
                entry.getPerformedBy(),
                entry.getPayload(),
                entry.getCreatedAt()
        ));
    }

    private Pageable withAuditIdTieBreaker(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.getOrderFor(AUDIT_ID) != null) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by(Sort.Order.desc(AUDIT_ID))));
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}
