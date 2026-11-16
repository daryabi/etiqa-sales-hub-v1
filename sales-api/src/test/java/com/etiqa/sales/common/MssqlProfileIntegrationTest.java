package com.etiqa.sales.common;

import com.etiqa.sales.auth.AuthResponse;
import com.etiqa.sales.auth.LoginRequest;
import com.etiqa.sales.common.web.ApiResponse;
import com.etiqa.sales.customer.CustomerRequest;
import com.etiqa.sales.customer.CustomerResponse;
import com.etiqa.sales.support.BrokerContainersConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "mssql"})
@Import(BrokerContainersConfiguration.class)
class MssqlProfileIntegrationTest {

    private static final String DATABASE = "TESTDB";
    private static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
                    .acceptLicense();

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient client;

    @BeforeAll
    static void enableHeadlessJasper() {
        System.setProperty("java.awt.headless", "true");
    }

    @DynamicPropertySource
    static void sqlServer(DynamicPropertyRegistry registry) throws SQLException {
        SQL_SERVER.start();
        try (Connection connection = DriverManager.getConnection(
                SQL_SERVER.getJdbcUrl(), SQL_SERVER.getUsername(), SQL_SERVER.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("IF DB_ID('" + DATABASE + "') IS NULL CREATE DATABASE " + DATABASE);
        }
        registry.add("spring.datasource.url", () -> SQL_SERVER.getJdbcUrl() + ";databaseName=" + DATABASE);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
    }

    @BeforeEach
    void createClient() {
        client = restClientBuilder.clone().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void schemaLoadsIntoTestDbAndCustomersWork() {
        assertThat(jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class)).isEqualTo(DATABASE);
        String token = client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("staff", "staff123"))
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<AuthResponse>>() { })
                .data().token();

        CustomerResponse withPersonalEmail = createCustomer(token,
                new CustomerRequest("Nurul", "Huda", null, "nurul.huda@example.com"));
        CustomerResponse noEmails = createCustomer(token, new CustomerRequest("Lim", "Wei", null, null));
        CustomerResponse alsoNoEmails = createCustomer(token, new CustomerRequest("Raj", "Kumar", null, null));

        CustomerResponse loaded = client.get().uri("/api/v1/customers/{id}", withPersonalEmail.customerId())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<CustomerResponse>>() { })
                .data();
        assertThat(loaded.emailPersonal()).isEqualTo("nurul.huda@example.com");
        assertThat(loaded.emailOffice()).isNull();
        assertThat(alsoNoEmails.customerId()).isNotEqualTo(noEmails.customerId());

        HttpStatusCode duplicate = client.post().uri("/api/v1/customers")
                .header("Authorization", "Bearer " + token)
                .body(new CustomerRequest("Copy", "Cat", null, "nurul.huda@example.com"))
                .exchange((request, response) -> response.getStatusCode());
        assertThat(duplicate).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO customers (first_name, last_name, email_personal) VALUES ('Raw', 'Insert', 'nurul.huda@example.com')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CustomerResponse createCustomer(String token, CustomerRequest request) {
        return client.post().uri("/api/v1/customers")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<CustomerResponse>>() { })
                .data();
    }
}
