package com.etiqa.sales.common.search;

import java.util.Locale;
import java.util.Optional;

public final class SearchPatterns {

    private SearchPatterns() {
    }

    public static Optional<String> toContainsLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return Optional.empty();
        }
        String escapedTerm = search.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return Optional.of("%" + escapedTerm + "%");
    }

    public static Optional<String> trimmedSearchTerm(String search) {
        if (search == null || search.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(search.trim());
    }
}
