package com.etiqa.sales.customer;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.common.web.PageResponse;
import com.etiqa.sales.support.TestMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final long CUSTOMER_ID = 7L;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, auditLogService, TestMessages.create());
    }

    @Test
    void create_trimsNamesAndBlankEmailsBecomeNull() {
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        CustomerResponse response = customerService.create(new CustomerRequest("  Alice ", " Tan ", "   ", null));

        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Tan");
        assertThat(response.emailOffice()).isNull();
        assertThat(response.emailPersonal()).isNull();
        verify(customerRepository, never()).existsByEmailOffice(anyString());
        verify(customerRepository, never()).existsByEmailPersonal(anyString());
        verify(auditLogService).record(AuditedResource.CUSTOMER, AuditAction.CREATE, CUSTOMER_ID, response);
    }

    @Test
    void create_rejectsTakenOfficeEmail() {
        when(customerRepository.existsByEmailOffice("alice@etiqa.com.my")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(
                new CustomerRequest("Alice", "Tan", " alice@etiqa.com.my ", null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Office email is already registered.");

        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsTakenPersonalEmail() {
        when(customerRepository.existsByEmailOffice("alice@etiqa.com.my")).thenReturn(false);
        when(customerRepository.existsByEmailPersonal("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(
                new CustomerRequest("Alice", "Tan", "alice@etiqa.com.my", "alice@example.com")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Personal email is already registered.");

        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_replacesAllFields() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.saveAndFlush(existing)).thenReturn(existing);

        CustomerResponse response = customerService.update(CUSTOMER_ID,
                new CustomerRequest(" Bob ", " Lee ", "bob@etiqa.com.my", ""));

        assertThat(response.firstName()).isEqualTo("Bob");
        assertThat(response.lastName()).isEqualTo("Lee");
        assertThat(response.emailOffice()).isEqualTo("bob@etiqa.com.my");
        assertThat(response.emailPersonal()).isNull();
        verify(auditLogService).record(AuditedResource.CUSTOMER, AuditAction.UPDATE, CUSTOMER_ID, response);
    }

    @Test
    void patch_onlyTouchesSentFields() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.saveAndFlush(existing)).thenReturn(existing);

        CustomerResponse response = customerService.patch(CUSTOMER_ID,
                new CustomerPatchRequest(null, " Lim ", null, " "));

        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Lim");
        assertThat(response.emailOffice()).isEqualTo("alice@etiqa.com.my");
        assertThat(response.emailPersonal()).isNull();
        verify(auditLogService).record(AuditedResource.CUSTOMER, AuditAction.PATCH, CUSTOMER_ID, response);
    }

    @Test
    void patch_withAllFields() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.saveAndFlush(existing)).thenReturn(existing);

        CustomerResponse response = customerService.patch(CUSTOMER_ID,
                new CustomerPatchRequest("Ali", "Lim", "ali@etiqa.com.my", "ali@example.com"));

        assertThat(response.firstName()).isEqualTo("Ali");
        assertThat(response.emailOffice()).isEqualTo("ali@etiqa.com.my");
        assertThat(response.emailPersonal()).isEqualTo("ali@example.com");
    }

    @Test
    void create_rejectsSameOfficeAndPersonalEmail() {
        assertThatThrownBy(() -> customerService.create(
                new CustomerRequest("Alice", "Tan", "alice@etiqa.com.my", "ALICE@etiqa.com.my")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Office and personal email must be different.");

        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_rejectsEmailUsedByAnotherCustomer() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer()));
        when(customerRepository.existsByEmailOfficeAndCustomerIdNot("taken@etiqa.com.my", CUSTOMER_ID)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(CUSTOMER_ID,
                new CustomerRequest("Alice", "Tan", "taken@etiqa.com.my", null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Office email is already registered.");

        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_keepingOwnEmailsIsFine() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.saveAndFlush(existing)).thenReturn(existing);

        customerService.update(CUSTOMER_ID,
                new CustomerRequest("Alice", "Tan", "alice@etiqa.com.my", "alice@example.com"));

        verify(customerRepository).existsByEmailOfficeAndCustomerIdNot("alice@etiqa.com.my", CUSTOMER_ID);
        verify(customerRepository).existsByEmailPersonalAndCustomerIdNot("alice@example.com", CUSTOMER_ID);
    }

    @Test
    void patch_rejectsEmailUsedByAnotherCustomer() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer()));
        when(customerRepository.existsByEmailPersonalAndCustomerIdNot("taken@example.com", CUSTOMER_ID)).thenReturn(true);

        assertThatThrownBy(() -> customerService.patch(CUSTOMER_ID,
                new CustomerPatchRequest(null, null, null, "taken@example.com")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Personal email is already registered.");

        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void patch_rejectsPersonalEmailMatchingCurrentOfficeEmail() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existingCustomer()));

        assertThatThrownBy(() -> customerService.patch(CUSTOMER_ID,
                new CustomerPatchRequest(null, null, null, "alice@etiqa.com.my")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Office and personal email must be different.");
    }

    @Test
    void create_raceOnOfficeEmail_becomesTheSameBusinessError() {
        when(customerRepository.saveAndFlush(any(Customer.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new RuntimeException("Duplicate entry 'a@etiqa.com.my' for key 'customers.email_office'")));

        assertThatThrownBy(() -> customerService.create(new CustomerRequest("Alice", "Tan", "a@etiqa.com.my", null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Office email is already registered.");
    }

    @Test
    void patch_raceOnPersonalEmail_becomesTheSameBusinessError() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(customerRepository.saveAndFlush(existing)).thenThrow(new DataIntegrityViolationException(
                "update failed", new RuntimeException("Violation of UNIQUE KEY constraint 'ux_customers_email_personal'")));

        assertThatThrownBy(() -> customerService.patch(CUSTOMER_ID, new CustomerPatchRequest(null, null, null, "b@example.com")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("Personal email is already registered.");
    }

    @Test
    void otherIntegrityProblemsAreNotRelabelled() {
        DataIntegrityViolationException otherProblem = new DataIntegrityViolationException(
                "insert failed", new RuntimeException("Data too long for column 'first_name'"));
        when(customerRepository.saveAndFlush(any(Customer.class))).thenThrow(otherProblem);

        assertThatThrownBy(() -> customerService.create(new CustomerRequest("Alice", "Tan", null, null)))
                .isSameAs(otherProblem);
    }

    @Test
    void findAllByIds_delegatesToRepository() {
        when(customerRepository.findAllById(List.of(CUSTOMER_ID))).thenReturn(List.of(existingCustomer()));

        assertThat(customerService.findAllByIds(List.of(CUSTOMER_ID))).hasSize(1);
    }

    @Test
    void delete_removesAndAudits() {
        Customer existing = existingCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));

        customerService.delete(CUSTOMER_ID);

        verify(customerRepository).delete(existing);
        verify(auditLogService).record(AuditedResource.CUSTOMER, AuditAction.DELETE, CUSTOMER_ID, null);
    }

    @Test
    void getById_unknownCustomerIsNotFound() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with ID: " + CUSTOMER_ID);
    }

    @Test
    void search_escapesWildcards() {
        Pageable pageable = PageRequest.of(0, 20);
        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        when(customerRepository.search(pattern.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existingCustomer()), pageable, 1));

        PageResponse<CustomerResponse> customerPage = customerService.getCustomers(" 50%_Off ", pageable);

        assertThat(pattern.getValue()).isEqualTo("%50\\%\\_off%");
        assertThat(customerPage.content()).extracting(CustomerResponse::customerId).containsExactly(CUSTOMER_ID);
    }

    @Test
    void list_withoutSearch() {
        Pageable pageable = PageRequest.of(0, 20);
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existingCustomer()), pageable, 1));

        assertThat(customerService.getCustomers(null, pageable).totalElements()).isEqualTo(1);
    }

    private static Customer existingCustomer() {
        return Customer.builder()
                .customerId(CUSTOMER_ID)
                .firstName("Alice")
                .lastName("Tan")
                .emailOffice("alice@etiqa.com.my")
                .emailPersonal("alice@example.com")
                .build();
    }

    private static Customer withId(Customer customer) {
        customer.setCustomerId(CUSTOMER_ID);
        return customer;
    }
}
