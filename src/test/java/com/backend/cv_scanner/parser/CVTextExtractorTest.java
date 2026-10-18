package com.backend.cv_scanner.parser;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class CVTextExtractorTest {

    private final CVTextExtractor extractor =
            new CVTextExtractor();

    @Test
    void shouldExtractTextFromPdf() throws Exception {

        try (InputStream inputStream =
                     getClass().getResourceAsStream("/sample.pdf")) {

            assertNotNull(inputStream, "Test PDF was not found");

            String text = extractor.extractText(
                    inputStream,
                    "sample.pdf"
            );

            assertNotNull(text);
            assertFalse(text.isBlank());
        }
    }
}