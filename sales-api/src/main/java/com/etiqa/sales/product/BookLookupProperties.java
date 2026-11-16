package com.etiqa.sales.product;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.book-lookup")
public record BookLookupProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {

    @Configuration
    static class OpenLibraryClientConfig {

        @Bean
        RestClient openLibraryRestClient(RestClient.Builder restClientBuilder, BookLookupProperties properties) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.connectTimeout());
            requestFactory.setReadTimeout(properties.readTimeout());
            return restClientBuilder.clone()
                    .baseUrl(properties.baseUrl())
                    .requestFactory(requestFactory)
                    .build();
        }
    }
}
