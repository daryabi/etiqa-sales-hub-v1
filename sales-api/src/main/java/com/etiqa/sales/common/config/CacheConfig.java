package com.etiqa.sales.common.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final String CACHE_FORMAT_VERSION = "v1:";

    public static RedisCacheConfiguration typedCache(ObjectMapper objectMapper, JavaType valueType, Duration timeToLive) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(CACHE_FORMAT_VERSION)
                .entryTtl(timeToLive)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, valueType)));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer transactionAwareCaches() {
        return builder -> builder.transactionAware();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
