package com.etiqa.sales.product;

import com.etiqa.sales.common.config.CacheConfig;
import com.etiqa.sales.common.web.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ProductCacheConfig {

    public static final String PRODUCTS_CACHE = "activeProducts";
    public static final String BOOK_LOOKUP_CACHE = "bookLookups";

    @Bean
    public RedisCacheManagerBuilderCustomizer productCaches(ObjectMapper objectMapper) {
        var typeFactory = objectMapper.getTypeFactory();
        return builder -> builder
                .withCacheConfiguration(PRODUCTS_CACHE, CacheConfig.typedCache(objectMapper,
                        typeFactory.constructParametricType(PageResponse.class, ProductResponse.class), Duration.ofHours(1)))
                .withCacheConfiguration(BOOK_LOOKUP_CACHE, CacheConfig.typedCache(objectMapper,
                        typeFactory.constructType(BookLookupResponse.class), Duration.ofHours(24)));
    }
}
