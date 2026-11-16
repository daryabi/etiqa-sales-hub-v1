package com.etiqa.sales.support;

import com.etiqa.sales.common.config.AppProperties;

import java.time.ZoneId;
import java.util.List;

public final class TestAppProperties {

    private TestAppProperties() {
    }

    public static AppProperties withDefaults() {
        return new AppProperties(
                ZoneId.of("Asia/Kuala_Lumpur"),
                10,
                366,
                List.of(),
                "https://errors.test/",
                new AppProperties.Topics("sales.recorded.v1", "inventory.stock-changed.v1")
        );
    }
}
