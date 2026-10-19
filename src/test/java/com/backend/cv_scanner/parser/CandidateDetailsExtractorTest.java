package com.backend.cv_scanner.parser;

import com.backend.cv_scanner.model.dto.CandidateDetailsDto;
import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CandidateDetailsExtractorTest {

    // fixed "today" so date ranges ending in "Present" give stable results
    private final CandidateDetailsExtractor extractor =
            new CandidateDetailsExtractor(Clock.fixed(
                    LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

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
        assertEquals("John Doe", result.fullName());
    }

    @Test
    void shouldReturnNullFieldsWhenTextIsEmpty() {

        CandidateDetailsDto  result = extractor.extract("");

        assertNull(result.fullName());
        assertNull(result.email());
        assertNull(result.phone());
        assertNull(result.yearsOfExperience());
        assertTrue(result.skills().isEmpty());
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
        assertNull(result.fullName());
    }

    @Test
    void shouldNotMistakeDateRangeForPhone() {

        CandidateDetailsDto result = extractor.extract("Acme Corp 2018 - 2020");

        assertNull(result.phone());
    }

    @Test
    void shouldNotJoinPhoneWithNextLine() {

        CandidateDetailsDto result = extractor.extract("+34 600 123 456\n3 years of experience in Python");

        assertEquals("+34 600 123 456", result.phone());
    }

    @Test
    void shouldIgnoreEducationPeriodsWhenThereIsNoExperienceSection() {

        String text = """
                Baku Higher Oil School                2024 - 2029 (Expected)
                Secondary School                      2013 - 2024
                Acme LLC, Backend Developer           2023 - 2025
                """;

        assertEquals(2, extractor.extract(text).yearsOfExperience());
    }

    @Test
    void shouldRecognizeExperienceHeadingWithExtraWords() {

        String text = """
                EDUCATION
                Secondary School   2013 - 2024
                EXPERIENCE & HACKATHONS
                Internship | Devlab LLC     September 2025
                Backend Developer, Initech  Jan 2024 - Jan 2025
                PROJECTS
                Side project 2010 - 2020
                """;

        assertEquals(1, extractor.extract(text).yearsOfExperience());
    }

        @Test
    void shouldExtractFullNameFromLabelOrUpperCaseHeader() {

        assertEquals("Leyla Mammadova", extractor.extract("Curriculum Vitae\nName: Leyla Mammadova\n").fullName());
        assertEquals("Anna Schmidt", extractor.extract("ANNA SCHMIDT\nSenior Data Engineer\n").fullName());
    }

    @Test
    void shouldPreferExplicitYearsOfExperience() {

        String text = """
                Software engineer with 7+ years of professional experience.
                Experience
                Acme 2022 - Present
                """;

        assertEquals(7, extractor.extract(text).yearsOfExperience());
    }

    @Test
    void shouldSumDateRangesInExperienceSectionWithoutOverlap() {

        String text = """
                Work Experience
                Backend Developer, Acme   Jan 2018 - Dec 2020
                Freelance Developer       2020 - 2021
                Senior Developer, Globex  03/2022 - Present
                Education
                BSc Computer Science      2010 - 2014
                """;

        // Jan 2018 -> Jan 2021 (merged, 36 months) + Mar 2022 -> Jan 2026 (46 months) = 82 months
        assertEquals(6, extractor.extract(text).yearsOfExperience());
    }

    @Test
    void shouldExtractSkillsWithoutFalsePositives() {

        String text = """
                Skills: Java, Spring Boot, C++, C#, .NET, Node.js, PostgreSQL, Docker, k8s
                Built single-page apps in JavaScript.
                """;

        var skills = extractor.extract(text).skills();

        assertTrue(skills.containsAll(java.util.List.of(
                "Java", "Spring Boot", "C++", "C#", ".NET", "Node.js", "PostgreSQL", "Docker", "Kubernetes", "JavaScript")));
        assertFalse(skills.contains("Go"));
        assertFalse(skills.contains("REST"));
    }

    @Test
    void shouldExtractLocationAndPreferences() {

        String text = """
                Jane Roe
                Location: Berlin, Germany
                Worked remotely for a US client 2019 - 2021.
                Preferences: open to hybrid, full-time positions
                """;

        CandidateDetailsDto result = extractor.extract(text);

        assertEquals("Berlin, Germany", result.location());
        assertEquals(JobLocationType.HYBRID, result.jobLocationType());
        assertEquals(JobType.FULL_TIME, result.jobType());
    }

    @Test
    void shouldDetectRemotePreference() {

        CandidateDetailsDto result = extractor.extract("Looking for a remote contract role. Based in Baku, Azerbaijan.");

        assertEquals(JobLocationType.REMOTE, result.jobLocationType());
        assertEquals(JobType.TEMPORARY, result.jobType());
        assertEquals("Baku, Azerbaijan", result.location());
    }
}
