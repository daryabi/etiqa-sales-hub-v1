package com.etiqa.sales.common.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemResponsesCustomizerTest {

    private OpenAPI openApi;

    @BeforeEach
    void setUp() {
        openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/sales", new PathItem().post(created()).get(ok()))
                .addPathItem("/api/v1/customers/{id}", new PathItem().get(ok()).delete(ok()))
                .addPathItem("/api/v1/auth/login", new PathItem().post(ok()))
                .addPathItem("/api/v1/products", new PathItem().get(ok()))
                .addPathItem("/api/v1/products/isbn/{isbn}", new PathItem().get(ok()))
                .addPathItem("/api/v1/audit-logs", new PathItem().get(ok())));

        new ProblemResponsesCustomizer().customise(openApi);
    }

    @Test
    void recordSale_documentsStockConflictsAndKeyReuse() {
        assertThat(codes("/api/v1/sales", PathItem.HttpMethod.POST))
                .containsExactlyInAnyOrder("201", "400", "401", "409", "422");
        assertThat(codes("/api/v1/sales", PathItem.HttpMethod.GET)).containsExactlyInAnyOrder("200", "400", "401");
    }

    @Test
    void deleteById_isAdminOnlyAndCanBeMissing() {
        assertThat(codes("/api/v1/customers/{id}", PathItem.HttpMethod.DELETE))
                .containsExactlyInAnyOrder("200", "400", "401", "403", "404", "409");
        assertThat(codes("/api/v1/audit-logs", PathItem.HttpMethod.GET)).contains("403");
    }

    @Test
    void publicEndpoints_needNoToken() {
        Operation login = operation("/api/v1/auth/login", PathItem.HttpMethod.POST);
        Operation catalog = operation("/api/v1/products", PathItem.HttpMethod.GET);

        assertThat(login.getSecurity()).isEmpty();
        assertThat(login.getResponses()).containsKeys("400", "401").doesNotContainKey("409");
        assertThat(catalog.getSecurity()).isEmpty();
        assertThat(catalog.getResponses()).doesNotContainKey("401");
    }

    @Test
    void isbnLookup_needsTokenAndCanFailUpstream() {
        Operation lookup = operation("/api/v1/products/isbn/{isbn}", PathItem.HttpMethod.GET);

        assertThat(lookup.getSecurity()).isNull();
        assertThat(lookup.getResponses()).containsKeys("401", "404", "502");
    }

    @Test
    void errorsPointAtTheSharedProblemSchema() {
        ApiResponse conflict = operation("/api/v1/sales", PathItem.HttpMethod.POST).getResponses().get("409");

        assertThat(conflict.getContent().get("application/problem+json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(openApi.getComponents().getSchemas()).containsKey(ProblemResponsesCustomizer.PROBLEM_SCHEMA);
    }

    @Test
    void eachStatusHasItsOwnExample() {
        Operation recordSale = operation("/api/v1/sales", PathItem.HttpMethod.POST);
        Operation getCustomer = operation("/api/v1/customers/{id}", PathItem.HttpMethod.GET);

        assertThat(example(recordSale, "422")).containsEntry("status", 422).containsEntry("title", "Idempotency Key Reused")
                .containsEntry("instance", "/api/v1/sales");
        assertThat(example(recordSale, "400")).containsEntry("status", 400).containsEntry("title", "Validation Failed");
        assertThat(example(getCustomer, "404")).containsEntry("status", 404).containsEntry("instance", "/api/v1/customers/42");
    }

    @Test
    void existingResponsesAreKept() {
        OpenAPI custom = new OpenAPI().paths(new Paths().addPathItem("/api/v1/sales",
                new PathItem().post(new Operation().responses(new ApiResponses()
                        .addApiResponse("409", new ApiResponse().description("custom"))))));

        new ProblemResponsesCustomizer().customise(custom);

        assertThat(custom.getPaths().get("/api/v1/sales").getPost().getResponses().get("409").getDescription())
                .isEqualTo("custom");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> example(Operation operation, String status) {
        return (Map<String, Object>) operation.getResponses().get(status).getContent().get("application/problem+json").getExample();
    }

    private Set<String> codes(String path, PathItem.HttpMethod method) {
        return operation(path, method).getResponses().keySet();
    }

    private Operation operation(String path, PathItem.HttpMethod method) {
        return openApi.getPaths().get(path).readOperationsMap().get(method);
    }

    private static Operation ok() {
        return new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK")));
    }

    private static Operation created() {
        return new Operation().responses(new ApiResponses().addApiResponse("201", new ApiResponse().description("Created")));
    }
}
