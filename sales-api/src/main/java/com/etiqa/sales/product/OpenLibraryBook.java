package com.etiqa.sales.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenLibraryBook(
        String title,
        List<String> publishers,
        @JsonProperty("publish_date") String publishDate,
        @JsonProperty("number_of_pages") Integer numberOfPages
) {
}
