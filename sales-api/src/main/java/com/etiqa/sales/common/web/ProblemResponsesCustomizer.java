package com.etiqa.sales.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProblemResponsesCustomizer implements OpenApiCustomizer {

    static final String PROBLEM_SCHEMA = "ProblemDetail";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String API = "/api/v1";
    private static final String LOGIN = API + "/auth/login";
    private static final String ERROR_BASE = "https://api.etiqa-sales-hub.local/errors/";
    private static final Map<String, String[]> EXAMPLES = Map.of(
            "400", new String[] {"validation", "Validation Failed", "One or more request fields failed validation."},
            "401", new String[] {"unauthorized", "Authentication Failed", "Valid JWT authentication is required to access this resource."},
            "403", new String[] {"forbidden", "Access Denied", "You do not have permission to access this resource."},
            "404", new String[] {"not-found", "Resource Not Found", "Customer not found with ID: 42"},
            "409", new String[] {"insufficient-stock", "Insufficient Stock", "Insufficient stock for book 'Effective Java'. Available: 3, Requested: 5"},
            "422", new String[] {"idempotency-key-reused", "Idempotency Key Reused",
                    "This Idempotency-Key was already used for a different sale. Use a new key for a new sale."},
            "502", new String[] {"upstream-unavailable", "External Service Unavailable", "Open Library did not answer in time."});

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, item) -> item.readOperationsMap()
                .forEach((method, operation) -> document(path, method, operation)));
    }

    private void document(String path, PathItem.HttpMethod method, Operation operation) {
        boolean isPublic = isPublic(path, method);
        if (isPublic) {
            operation.setSecurity(List.of());
        }
        boolean changesData = method != PathItem.HttpMethod.GET;

        String instance = path.replaceAll("\\{[^}]+}", "42");
        add(operation, instance, "400", "The request is invalid: a field failed validation or a parameter has the wrong format");
        if (!isPublic || LOGIN.equals(path)) {
            add(operation, instance, "401", "No valid token was sent, or the username or password is wrong");
        }
        if (isAdminOnly(path, method)) {
            add(operation, instance, "403", "Signed in, but only an ADMIN can do this");
        }
        if (path.contains("{")) {
            add(operation, instance, "404", "Nothing exists with that id");
        }
        if (changesData && !LOGIN.equals(path)) {
            add(operation, instance, "409", "Conflicts with the current data, e.g. not enough stock, a taken username, or a record still in use");
        }
        if (method == PathItem.HttpMethod.POST && (API + "/sales").equals(path)) {
            add(operation, instance, "422", "This Idempotency-Key was already used for a different sale");
        }
        if (path.startsWith(API + "/products/isbn/")) {
            add(operation, instance, "502", "Open Library could not be reached or answered with an error");
        }
    }

    private static boolean isPublic(String path, PathItem.HttpMethod method) {
        if (path.startsWith(API + "/auth/")) {
            return true;
        }
        return method == PathItem.HttpMethod.GET
                && path.startsWith(API + "/products")
                && !path.startsWith(API + "/products/isbn/");
    }

    private static boolean isAdminOnly(String path, PathItem.HttpMethod method) {
        return method == PathItem.HttpMethod.DELETE || path.startsWith(API + "/audit-logs");
    }

    private static void add(Operation operation, String instance, String status, String description) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        if (operation.getResponses().containsKey(status)) {
            return;
        }
        Schema<Object> reference = new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA);
        operation.getResponses().addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(reference).example(example(instance, status)))));
    }

    private static Map<String, Object> example(String instance, String status) {
        String[] problem = EXAMPLES.get(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", ERROR_BASE + problem[0]);
        body.put("title", problem[1]);
        body.put("status", Integer.parseInt(status));
        body.put("detail", problem[2]);
        body.put("instance", instance);
        body.put("timestamp", "2026-11-16T03:57:14Z");
        body.put("correlationId", "6874eeb1-ab2d-434e-9381-2083b95be4a7");
        return body;
    }

    private static Schema<Object> problemSchema() {
        return new ObjectSchema()
                .description("RFC 7807 problem response returned for every error")
                .addProperty("type", new StringSchema().format("uri"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("correlationId", new StringSchema());
    }
}
