package com.etiqa.sales.report;

import java.util.Arrays;

public record PdfDocument(String filename, byte[] content) {

    public PdfDocument {
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
