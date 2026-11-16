package com.etiqa.sales.auth;

import com.etiqa.sales.common.exception.ErrorType;
import com.etiqa.sales.common.exception.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String PROMETHEUS_ENDPOINT = "/actuator/prometheus";
    private static final String METRICS_SCRAPER_ROLE = "METRICS_SCRAPER";

    @Bean
    @Order(1)
    public SecurityFilterChain metricsScrapeFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        http
                .securityMatcher(PROMETHEUS_ENDPOINT)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(metricsScraperAccounts(properties.metrics(), passwordEncoder))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(authorization -> authorization.anyRequest().hasRole(METRICS_SCRAPER_ROLE));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            CorsConfigurationSource corsConfigurationSource,
            ProblemDetailFactory problemDetailFactory,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authenticationException) ->
                                sendProblem(response, problemDetailFactory.create(ErrorType.MISSING_TOKEN), objectMapper)
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                sendProblem(response, problemDetailFactory.create(ErrorType.FORBIDDEN), objectMapper)
                        )
                )
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/isbn/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**", "/api/v1/audit-logs/**").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private InMemoryUserDetailsManager metricsScraperAccounts(SecurityProperties.Metrics metrics, PasswordEncoder passwordEncoder) {
        if (!metrics.scrapeEnabled()) {
            return new InMemoryUserDetailsManager();
        }
        return new InMemoryUserDetailsManager(User.withUsername(metrics.username())
                .password(passwordEncoder.encode(metrics.password()))
                .roles(METRICS_SCRAPER_ROLE)
                .build());
    }

    private void sendProblem(HttpServletResponse response, ProblemDetail problem, ObjectMapper objectMapper)
            throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
