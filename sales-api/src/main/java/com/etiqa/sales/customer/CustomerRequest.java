package com.etiqa.sales.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = Customer.NAME_MAX_LENGTH) String firstName,
        @NotBlank @Size(max = Customer.NAME_MAX_LENGTH) String lastName,
        @Email @Size(max = Customer.EMAIL_MAX_LENGTH) String emailOffice,
        @Email @Size(max = Customer.EMAIL_MAX_LENGTH) String emailPersonal
) {
}
