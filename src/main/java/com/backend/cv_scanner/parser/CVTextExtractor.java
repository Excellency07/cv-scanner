package com.backend.cv_scanner.parser;

import com.backend.cv_scanner.exception.CVParsingException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class CVTextExtractor {

    private static final int MAX_LENGTH = 1000000;
    private final Tika tika =  new Tika();

    public String extractText(
            InputStream inputStream,
            String filename
    ) throws IOException {

        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);

        String text;
        try {
            text = tika.parseToString(
                    inputStream,
                    metadata,
                    MAX_LENGTH
            );
        } catch (TikaException e) {
            throw new CVParsingException("Failed to extract text from " + filename, e);
        }

        if (text == null || text.isBlank()) {
            throw new CVParsingException("No readable text found in " + filename);
        }

        return text;
    }

}
