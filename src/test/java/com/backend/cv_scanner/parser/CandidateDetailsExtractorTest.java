package com.backend.cv_scanner.parser;

import com.backend.cv_scanner.model.dto.CandidateDetailsDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CandidateDetailsExtractorTest {

    private final CandidateDetailsExtractor extractor =
            new CandidateDetailsExtractor();

    @Test
    void shouldExtractEmailAndPhone() {

        String text = """
                John Doe
                Backend Developer
                Email: john.doe@gmail.com
                Phone: +994 50 123 45 67
                Skills: Java, Spring Boot, PostgreSQL
                """;

        CandidateDetailsDto result = extractor.extract(text);

        assertEquals("john.doe@gmail.com", result.email());
        assertEquals("+994 50 123 45 67", result.phone());
        assertNull(result.fullName());
    }

    @Test
    void shouldReturnNullFieldsWhenTextIsEmpty() {

        CandidateDetailsDto  result = extractor.extract("");

        assertNull(result.fullName());
        assertNull(result.email());
        assertNull(result.phone());
    }

    @Test
    void shouldHandleMissingEmailAndPhone() {

        String text = """
                Java Backend Developer
                Skills: Java, Spring Boot
                """;

        CandidateDetailsDto result = extractor.extract(text);

        assertNull(result.email());
        assertNull(result.phone());
    }
}
