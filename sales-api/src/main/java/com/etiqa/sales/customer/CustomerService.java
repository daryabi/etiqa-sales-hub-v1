package com.etiqa.sales.customer;

import com.etiqa.sales.audit.AuditAction;
import com.etiqa.sales.audit.AuditLogService;
import com.etiqa.sales.audit.AuditedResource;
import com.etiqa.sales.common.exception.BusinessValidationException;
import com.etiqa.sales.common.exception.ResourceNotFoundException;
import com.etiqa.sales.common.i18n.MessageKey;
import com.etiqa.sales.common.i18n.Messages;
import com.etiqa.sales.common.search.SearchPatterns;
import com.etiqa.sales.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final Messages messages;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String officeEmail = blankToNull(request.emailOffice());
        String personalEmail = blankToNull(request.emailPersonal());
        validateCustomerEmails(null, officeEmail, personalEmail);

        Customer customer = Customer.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .emailOffice(officeEmail)
                .emailPersonal(personalEmail)
                .build();

        CustomerResponse response = CustomerResponse.from(saveCustomer(customer));
        auditLogService.record(AuditedResource.CUSTOMER, AuditAction.CREATE, response.customerId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long customerId) {
        return CustomerResponse.from(getCustomerOrThrow(customerId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(String search, Pageable pageable) {
        Page<Customer> page = SearchPatterns.toContainsLikePattern(search)
                .map(likePattern -> customerRepository.search(likePattern, pageable))
                .orElseGet(() -> customerRepository.findAll(pageable));
        return PageResponse.from(page, CustomerResponse::from);
    }

    @Transactional
    public CustomerResponse update(Long customerId, CustomerRequest request) {
        Customer customer = getCustomerOrThrow(customerId);
        String officeEmail = blankToNull(request.emailOffice());
        String personalEmail = blankToNull(request.emailPersonal());
        validateCustomerEmails(customerId, officeEmail, personalEmail);

        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmailOffice(officeEmail);
        customer.setEmailPersonal(personalEmail);

        CustomerResponse response = CustomerResponse.from(saveCustomer(customer));
        auditLogService.record(AuditedResource.CUSTOMER, AuditAction.UPDATE, response.customerId(), response);
        return response;
    }

    @Transactional
    public CustomerResponse patch(Long customerId, CustomerPatchRequest request) {
        Customer customer = getCustomerOrThrow(customerId);
        if (request.firstName() != null) {
            customer.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName().trim());
        }
        String officeEmail = request.emailOffice() != null ? blankToNull(request.emailOffice()) : customer.getEmailOffice();
        String personalEmail = request.emailPersonal() != null ? blankToNull(request.emailPersonal()) : customer.getEmailPersonal();
        validateCustomerEmails(customerId, officeEmail, personalEmail);
        customer.setEmailOffice(officeEmail);
        customer.setEmailPersonal(personalEmail);

        CustomerResponse response = CustomerResponse.from(saveCustomer(customer));
        auditLogService.record(AuditedResource.CUSTOMER, AuditAction.PATCH, response.customerId(), response);
        return response;
    }

    @Transactional
    public void delete(Long customerId) {
        Customer customer = getCustomerOrThrow(customerId);
        customerRepository.delete(customer);
        auditLogService.record(AuditedResource.CUSTOMER, AuditAction.DELETE, customerId, null);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAllByIds(Collection<Long> customerIds) {
        return customerRepository.findAllById(customerIds);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get(MessageKey.CUSTOMER_NOT_FOUND, customerId)));
    }

    private Customer saveCustomer(Customer customer) {
        try {
            return customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException exception) {
            String violation = String.valueOf(exception.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (violation.contains("email_office")) {
                throw new BusinessValidationException(messages.get(MessageKey.CUSTOMER_OFFICE_EMAIL_DUPLICATE), exception);
            }
            if (violation.contains("email_personal")) {
                throw new BusinessValidationException(messages.get(MessageKey.CUSTOMER_PERSONAL_EMAIL_DUPLICATE), exception);
            }
            throw exception;
        }
    }

    private void validateCustomerEmails(Long excludeCustomerId, String officeEmail, String personalEmail) {
        if (officeEmail != null && officeEmail.equalsIgnoreCase(personalEmail)) {
            throw new BusinessValidationException(messages.get(MessageKey.CUSTOMER_EMAILS_IDENTICAL));
        }
        if (officeEmail != null && officeEmailTaken(officeEmail, excludeCustomerId)) {
            throw new BusinessValidationException(messages.get(MessageKey.CUSTOMER_OFFICE_EMAIL_DUPLICATE));
        }
        if (personalEmail != null && personalEmailTaken(personalEmail, excludeCustomerId)) {
            throw new BusinessValidationException(messages.get(MessageKey.CUSTOMER_PERSONAL_EMAIL_DUPLICATE));
        }
    }

    private boolean officeEmailTaken(String officeEmail, Long excludeCustomerId) {
        return excludeCustomerId == null
                ? customerRepository.existsByEmailOffice(officeEmail)
                : customerRepository.existsByEmailOfficeAndCustomerIdNot(officeEmail, excludeCustomerId);
    }

    private boolean personalEmailTaken(String personalEmail, Long excludeCustomerId) {
        return excludeCustomerId == null
                ? customerRepository.existsByEmailPersonal(personalEmail)
                : customerRepository.existsByEmailPersonalAndCustomerIdNot(personalEmail, excludeCustomerId);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
