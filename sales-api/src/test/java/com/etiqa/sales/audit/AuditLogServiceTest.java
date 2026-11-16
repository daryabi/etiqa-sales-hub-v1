package com.etiqa.sales.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository, new ObjectMapper());
    }

    @Test
    void sameSecondEntriesAreOrderedByAuditIdToo() {
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        auditLogService.findAll(Optional.empty(), PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))));

        ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(used.capture());
        assertThat(used.getValue().getSort())
                .containsExactly(Sort.Order.desc("createdAt"), Sort.Order.desc("auditId"));
        assertThat(used.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void explicitAuditIdSortIsLeftAlone() {
        Pageable oldestFirst = PageRequest.of(1, 20, Sort.by(Sort.Order.asc("auditId")));
        when(auditLogRepository.findByEntityName(eq("SALE"), any(Pageable.class))).thenReturn(Page.empty());

        auditLogService.findAll(Optional.of(AuditedResource.SALE), oldestFirst);

        verify(auditLogRepository).findByEntityName("SALE", oldestFirst);
    }
}
