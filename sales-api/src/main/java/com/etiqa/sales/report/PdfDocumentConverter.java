package com.etiqa.sales.report;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfDocumentConverter extends AbstractHttpMessageConverter<PdfDocument> {

    public PdfDocumentConverter() {
        super(MediaType.APPLICATION_PDF);
    }

    @Override
    protected boolean supports(Class<?> requestedType) {
        return PdfDocument.class.isAssignableFrom(requestedType);
    }

    @Override
    protected PdfDocument readInternal(Class<? extends PdfDocument> requestedType, HttpInputMessage inputMessage) {
        throw new UnsupportedOperationException("PDF documents cannot be read from a request body");
    }

    @Override
    protected void writeInternal(PdfDocument pdfDocument, HttpOutputMessage outputMessage) throws IOException {
        byte[] content = pdfDocument.content();
        outputMessage.getHeaders().setContentDisposition(ContentDisposition.attachment().filename(pdfDocument.filename()).build());
        outputMessage.getHeaders().setContentLength(content.length);
        outputMessage.getBody().write(content);
    }
}
