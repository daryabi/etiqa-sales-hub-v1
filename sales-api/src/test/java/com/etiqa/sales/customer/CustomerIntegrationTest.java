package com.etiqa.sales.customer;

import com.etiqa.sales.support.ApiIntegrationTest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerIntegrationTest extends ApiIntegrationTest {

    @Test
    void customerLifecycle() {
        Consumer<HttpHeaders> staff = bearer(login("staff", "staff123"));
        Consumer<HttpHeaders> admin = bearer(login("admin", "admin123"));
        CustomerResponse created = createCustomer(staff, "lifecycle");
        String customerUri = "/api/v1/customers/" + created.customerId();

        assertThat(data(call(HttpMethod.GET, customerUri, staff, null, envelope(CustomerResponse.class))).firstName())
                .isEqualTo("Customer");

        CustomerResponse replaced = data(call(HttpMethod.PUT, customerUri, staff,
                new CustomerRequest("Nur", "Aisyah", null, "nur.aisyah@example.com"), envelope(CustomerResponse.class)));
        assertThat(replaced.emailOffice()).isNull();

        CustomerResponse patched = data(call(HttpMethod.PATCH, customerUri, staff,
                new CustomerPatchRequest(null, "Rahman", null, null), envelope(CustomerResponse.class)));
        assertThat(patched.firstName()).isEqualTo("Nur");
        assertThat(patched.lastName()).isEqualTo("Rahman");
        assertThat(patched.emailPersonal()).isEqualTo("nur.aisyah@example.com");

        Reply<Object> duplicateEmail = call(HttpMethod.POST, "/api/v1/customers", staff,
                new CustomerRequest("Someone", "Else", null, "nur.aisyah@example.com"), ANY_BODY);
        assertThat(duplicateEmail.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicateEmail.problem().getTitle()).isEqualTo("Business Rule Violation");

        CustomerResponse someoneElse = createCustomer(staff, "someone-else");
        Reply<Object> patchToTakenEmail = call(HttpMethod.PATCH, "/api/v1/customers/" + someoneElse.customerId(), staff,
                new CustomerPatchRequest(null, null, null, "nur.aisyah@example.com"), ANY_BODY);
        assertThat(patchToTakenEmail.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patchToTakenEmail.problem().getDetail()).isEqualTo("Personal email is already registered.");

        Reply<Object> blankName = call(HttpMethod.POST, "/api/v1/customers", staff,
                new CustomerRequest(" ", "Else", "not-an-email", null), ANY_BODY);
        assertThat(blankName.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blankName.problem().getProperties().get("errors"))
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsKeys("firstName", "emailOffice");

        assertThat(call(HttpMethod.DELETE, customerUri, staff, null, ANY_BODY).status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, customerUri, admin, null, ANY_BODY).status()).isEqualTo(HttpStatus.NO_CONTENT);

        Reply<Object> gone = call(HttpMethod.GET, customerUri, staff, null, ANY_BODY);
        assertThat(gone.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(gone.problem().getTitle()).isEqualTo("Resource Not Found");
        assertThat(call(HttpMethod.GET, "/api/v1/products/999999", staff, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/api/v1/sales/999999", staff, null, ANY_BODY).status())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
