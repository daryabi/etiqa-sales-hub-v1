package com.etiqa.sales.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        ZoneId timezone,
        int lowStockThreshold,
        @DefaultValue("366") int maxReportDays,
        @DefaultValue List<String> corsAllowedOrigins,
        String errorTypeBaseUri,
        Topics topics
) {

    public record Topics(String sales, String stock) {
    }
}
