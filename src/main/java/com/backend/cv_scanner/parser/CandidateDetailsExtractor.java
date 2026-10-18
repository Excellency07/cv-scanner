package com.backend.cv_scanner.parser;

import com.backend.cv_scanner.model.dto.CandidateDetailsDto;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CandidateDetailsExtractor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\w)(?:\\+?\\d[\\d\\s().-]{6,18}\\d)(?!\\w)");

    public CandidateDetailsDto extract(String text) {

        if (text == null || text.isBlank()) {
            return new CandidateDetailsDto(null, null, null);
        }

        String extractedEmail = extractEmail(text);
        String extractedPhone = extractPhone(text);
        return new CandidateDetailsDto(null, extractedEmail, extractedPhone);
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);

        if (matcher.find()) {
            return matcher.group().trim();
        }

        return null;
    }

    public String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        return null;
    }
}
