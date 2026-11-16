package com.etiqa.sales.customer;

import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Endpoints for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new customer")
    public ApiResponse<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.of("Customer created", customerService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a customer by ID")
    public ApiResponse<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List customers page by page, optionally filtered by name or email")
    public ApiResponse<PageResponse<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "customerId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(customerService.getCustomers(search, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing customer's details")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        return ApiResponse.of("Customer updated", customerService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a customer's details")
    public ApiResponse<CustomerResponse> patchCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerPatchRequest request
    ) {
        return ApiResponse.of("Customer updated", customerService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a customer by ID")
    public void deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
    }
}
