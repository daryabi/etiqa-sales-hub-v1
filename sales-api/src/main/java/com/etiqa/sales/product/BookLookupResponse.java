package com.etiqa.sales.product;

import java.util.List;

public record BookLookupResponse(
        String isbn,
        String title,
        List<String> publishers,
        String publishDate,
        Integer numberOfPages
) {
}
