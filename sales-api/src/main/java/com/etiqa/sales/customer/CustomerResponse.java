package com.etiqa.sales.customer;

import java.time.Instant;

public record CustomerResponse(
        Long customerId,
        String firstName,
        String lastName,
        String emailOffice,
        String emailPersonal,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmailOffice(),
                customer.getEmailPersonal(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
