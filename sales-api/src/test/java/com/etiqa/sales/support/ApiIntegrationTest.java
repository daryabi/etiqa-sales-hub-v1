package com.etiqa.sales.support;

import com.etiqa.sales.audit.AuditLogRepository;
import com.etiqa.sales.auth.AuthResponse;
import com.etiqa.sales.auth.LoginRequest;
import com.etiqa.sales.auth.UserAccountRepository;
import com.etiqa.sales.auth.SecurityProperties;
import com.etiqa.sales.common.config.AppProperties;
import com.etiqa.sales.product.ProductCacheConfig;
import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.common.web.PageResponse;
import com.etiqa.sales.customer.CustomerRequest;
import com.etiqa.sales.customer.CustomerResponse;
import com.etiqa.sales.product.ProductImportResponse;
import com.etiqa.sales.product.ProductRequest;
import com.etiqa.sales.product.ProductResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureObservability(tracing = false)
@Import(TestcontainersConfiguration.class)
public abstract class ApiIntegrationTest {

    protected static final ParameterizedTypeReference<Object> ANY_BODY = new ParameterizedTypeReference<>() { };
    protected static final ParameterizedTypeReference<byte[]> BYTES = new ParameterizedTypeReference<>() { };
    protected static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(30);

    @LocalServerPort
    protected int port;

    @Autowired
    protected RestClient.Builder restClientBuilder;

    @Autowired
    protected SecurityProperties securityProperties;

    @Autowired
    protected AppProperties appProperties;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @Autowired
    protected UserAccountRepository userAccountRepository;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    protected ConsumerFactory<String, String> consumerFactory;

    protected RestClient client;

    @BeforeAll
    static void enableHeadlessJasper() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void createClient() {
        client = restClientBuilder.clone().baseUrl("http://localhost:" + port).build();
    }

    protected record Reply<T>(HttpStatusCode status, HttpHeaders headers, T body, ProblemDetail problem) {
    }

    protected <T> Reply<T> call(
            HttpMethod method,
            String uri,
            Consumer<HttpHeaders> headers,
            Object body,
            ParameterizedTypeReference<T> bodyType
    ) {
        RestClient.RequestBodySpec request = client.method(method).uri(uri).headers(headers);
        if (body != null) {
            request.body(body);
        }
        return request.exchange((clientRequest, response) -> {
            HttpStatusCode status = response.getStatusCode();
            if (status.isError()) {
                return new Reply<>(status, response.getHeaders(), null, response.bodyTo(ProblemDetail.class));
            }
            T payload = status.value() == HttpStatus.NO_CONTENT.value() ? null : response.bodyTo(bodyType);
            return new Reply<>(status, response.getHeaders(), payload, null);
        });
    }

    protected static <T> T data(Reply<ApiResponse<T>> reply) {
        assertThat(reply.status().is2xxSuccessful()).as("status %s, problem %s", reply.status(), reply.problem()).isTrue();
        return reply.body().data();
    }

    protected static <T> ParameterizedTypeReference<ApiResponse<T>> envelope(Class<T> dataType) {
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(ApiResponse.class, dataType).getType()
        );
    }

    protected static <T> ParameterizedTypeReference<ApiResponse<PageResponse<T>>> pageEnvelope(Class<T> itemType) {
        ResolvableType pageType = ResolvableType.forClassWithGenerics(PageResponse.class, itemType);
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(ApiResponse.class, pageType).getType()
        );
    }

    protected static Consumer<HttpHeaders> noAuth() {
        return headers -> { };
    }

    protected static Consumer<HttpHeaders> bearer(String token) {
        return headers -> headers.setBearerAuth(token);
    }

    protected String login(String username, String password) {
        return data(call(HttpMethod.POST, "/api/v1/auth/login", noAuth(),
                new LoginRequest(username, password), envelope(AuthResponse.class))).token();
    }

    protected CustomerResponse createCustomer(Consumer<HttpHeaders> auth, String label) {
        String unique = label + "-" + UUID.randomUUID().toString().substring(0, 8);
        return data(call(HttpMethod.POST, "/api/v1/customers", auth,
                new CustomerRequest("Customer", unique, unique + "@etiqa.com.my", null), envelope(CustomerResponse.class)));
    }

    protected ProductResponse createBook(Consumer<HttpHeaders> auth, String title, String price, int stock) {
        Reply<ApiResponse<ProductResponse>> created = call(HttpMethod.POST, "/api/v1/products", auth,
                new ProductRequest(title, new BigDecimal(price), stock), envelope(ProductResponse.class));
        assertThat(created.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.body().message()).isEqualTo("Book added");
        return created.body().data();
    }

    protected ProductResponse getBook(Consumer<HttpHeaders> auth, long bookId) {
        return data(call(HttpMethod.GET, "/api/v1/products/" + bookId, auth, null, envelope(ProductResponse.class)));
    }

    protected Set<String> cachedCatalogPages() {
        return redisTemplate.keys("*" + ProductCacheConfig.PRODUCTS_CACHE + "::*");
    }

    protected PageResponse<ProductResponse> getProducts(String uri) {
        return data(call(HttpMethod.GET, uri, noAuth(), null, pageEnvelope(ProductResponse.class)));
    }

    protected Reply<ApiResponse<ProductImportResponse>> uploadBooks(Consumer<HttpHeaders> auth, String pipeData) {
        ByteArrayResource file = new ByteArrayResource(pipeData.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "books-batch.txt";
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", file);
        return call(HttpMethod.POST, "/api/v1/products/upload",
                auth.andThen(headers -> headers.setContentType(MediaType.MULTIPART_FORM_DATA)),
                form, envelope(ProductImportResponse.class));
    }
}
