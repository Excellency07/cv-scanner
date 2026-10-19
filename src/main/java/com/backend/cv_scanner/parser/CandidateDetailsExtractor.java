package com.backend.cv_scanner.parser;

import com.backend.cv_scanner.model.dto.CandidateDetailsDto;
import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Rule-based extraction of candidate details from plain CV text.
 * Every field is best-effort: when nothing reliable is found the field is left null.
 */
@Component
public class CandidateDetailsExtractor {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\w)(?:\\+?\\d[\\d \\t().-]{6,18}\\d)(?!\\w)");

    // real phone numbers have 9-15 digits; this keeps date ranges like "2018 - 2020" out
    private static final int MIN_PHONE_DIGITS = 9;
    private static final int MAX_PHONE_DIGITS = 15;

    // ---- Full name ----

    private static final int NAME_SEARCH_LINES = 8;

    private static final Pattern NAME_LABEL_PATTERN =
            Pattern.compile("^\\s*(?:full\\s*name|name)\\s*[:\\-–]\\s*(.+)$", FLAGS | Pattern.MULTILINE);

    // 2-4 words, each starting with an upper-case letter (also matches "JOHN DOE")
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^\\p{Lu}[\\p{L}'’-]+(?:\\s+\\p{Lu}[\\p{L}'’.-]*){1,3}$");

    private static final Set<String> NAME_STOP_WORDS = Set.of(
            "curriculum", "vitae", "resume", "résumé", "cv", "profile", "summary", "contact", "about",
            "objective", "experience", "education", "skills", "developer", "engineer", "manager",
            "analyst", "designer", "consultant", "specialist", "intern", "senior", "junior", "lead",
            "architect", "administrator", "scientist", "programmer"
    );

    // ---- Years of experience ----

    private static final int MAX_YEARS = 50;

    private static final Pattern[] EXPLICIT_YEARS_PATTERNS = {
            // "5+ years of experience", "7 years of professional experience", "3 yrs experience"
            Pattern.compile("(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)(?:\\s+of)?(?:\\s+[\\p{L}-]+){0,3}?\\s+experience", FLAGS),
            // "Experience: 5 years", "experience of 4+ years"
            Pattern.compile("experience\\s*(?:of|:|-|–)?\\s*(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)", FLAGS)
    };

    private static final String MONTH =
            "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?";

    // "Jan 2019 - Present", "03/2018 – 05/2021", "2015 to 2019"
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "(?:(" + MONTH + ")\\.?\\s+|(\\d{1,2})[/.])?((?:19|20)\\d{2})" +
                    "\\s*(?:-|–|—|to|until)\\s*" +
                    "(?:(?:(" + MONTH + ")\\.?\\s+|(\\d{1,2})[/.])?((?:19|20)\\d{2})|(present|current|now|today|ongoing))",
            FLAGS);

    // a heading may carry extra words: "EXPERIENCE & HACKATHONS", "Education / Certifications"
    private static final String HEADING_SUFFIX = "(?:\\s*[&/,]\\s*[\\p{L} ]{1,30})?\\s*:?\\s*$";

    private static final Pattern EXPERIENCE_HEADING = Pattern.compile(
            "^\\s*(?:(?:work|professional|relevant)\\s+)?(?:experience|employment(?:\\s+history)?|work\\s+history|career\\s+history)" + HEADING_SUFFIX,
            FLAGS);

    private static final Pattern OTHER_HEADING = Pattern.compile(
            "^\\s*(?:education|(?:technical\\s+)?skills|projects|certifications?|languages|interests|hobbies|references|awards|achievements|publications|volunteer(?:ing)?|summary|profile|courses|training)" + HEADING_SUFFIX,
            FLAGS);

    // without an experience section, school/university periods must not count as work experience
    private static final Pattern EDUCATION_LINE = Pattern.compile(
            "\\b(?:school|universit(?:y|ies)|college|academy|institute|bachelor|master|ph\\.?d|b\\.?sc|m\\.?sc|degree|diploma|lyceum|gymnasium|expected)\\b",
            FLAGS);

    // ---- Location & preferences ----

    private static final Pattern LOCATION_LABEL_PATTERN = Pattern.compile(
            "^\\s*(?:current\\s+location|location|address|city|residence|based\\s+in)\\s*[:\\-–]\\s*(.+)$",
            FLAGS | Pattern.MULTILINE);

    private static final Pattern BASED_IN_PATTERN =
            Pattern.compile("\\b(?i:based\\s+in)\\s+(\\p{Lu}[\\p{L} ,.'-]{1,60})");

    private static final Pattern PREFERENCE_LINE = Pattern.compile(
            "prefer|preference|looking\\s+for|open\\s+to|seeking|available\\s+for|desired|work\\s+(?:mode|type|arrangement|style)|employment\\s+type",
            FLAGS);

    private static final Map<JobLocationType, Pattern> LOCATION_TYPE_PATTERNS = new LinkedHashMap<>();
    private static final Map<JobType, Pattern> JOB_TYPE_PATTERNS = new LinkedHashMap<>();

    static {
        LOCATION_TYPE_PATTERNS.put(JobLocationType.HYBRID, Pattern.compile("\\bhybrid\\b", FLAGS));
        LOCATION_TYPE_PATTERNS.put(JobLocationType.REMOTE, Pattern.compile("\\bremote(?:ly)?\\b|work\\s+from\\s+home", FLAGS));
        LOCATION_TYPE_PATTERNS.put(JobLocationType.ON_SITE, Pattern.compile("\\bon[\\s-]?site\\b|\\bin[\\s-]office\\b|\\boffice[\\s-]based\\b", FLAGS));

        JOB_TYPE_PATTERNS.put(JobType.FULL_TIME, Pattern.compile("\\bfull[\\s-]?time\\b|\\bpermanent\\b", FLAGS));
        JOB_TYPE_PATTERNS.put(JobType.PART_TIME, Pattern.compile("\\bpart[\\s-]?time\\b", FLAGS));
        JOB_TYPE_PATTERNS.put(JobType.INTERNSHIP, Pattern.compile("\\binternship\\b|\\bintern\\b", FLAGS));
        JOB_TYPE_PATTERNS.put(JobType.TEMPORARY, Pattern.compile("\\btemporary\\b|\\bcontract(?:or)?\\b|\\bfreelance(?:r)?\\b|\\bfixed[\\s-]term\\b", FLAGS));
    }

    // ---- Skills ----

    private static final Map<String, Pattern> SKILL_PATTERNS = buildSkillPatterns(new String[][]{
            {"Java"}, {"Kotlin"}, {"Scala"}, {"Groovy"}, {"Python"},
            {"JavaScript", "javascript", "js"}, {"TypeScript"},
            {"C++", "c++", "cpp"}, {"C#", "c#", "csharp"}, {".NET", ".net", "dotnet", "asp.net"},
            {"Go", "golang"}, {"Rust"}, {"Ruby"}, {"PHP"}, {"Swift"}, {"Objective-C"}, {"Dart"},
            {"SQL"}, {"PL/SQL"}, {"Bash", "bash", "shell scripting"},
            {"Spring Boot"}, {"Spring", "spring framework", "spring"}, {"Spring Batch"}, {"Spring Security"},
            {"Hibernate"}, {"JPA"}, {"Microservices", "microservices", "microservice"},
            {"REST", "rest api", "rest apis", "restful"}, {"GraphQL"}, {"gRPC"},
            {"Kafka", "kafka"}, {"RabbitMQ"},
            {"Node.js", "node.js", "nodejs"}, {"Express", "express.js", "expressjs"},
            {"React", "react", "react.js", "reactjs"}, {"React Native"}, {"Angular"}, {"Vue.js", "vue", "vue.js", "vuejs"},
            {"Next.js"}, {"Django"}, {"Flask"}, {"FastAPI"}, {"Laravel"}, {"Ruby on Rails", "ruby on rails", "rails"},
            {"HTML", "html", "html5"}, {"CSS", "css", "css3"}, {"Tailwind CSS", "tailwind"},
            {"PostgreSQL", "postgresql", "postgres"}, {"MySQL"}, {"Oracle"}, {"MongoDB", "mongodb", "mongo"},
            {"Redis"}, {"Elasticsearch"}, {"Cassandra"}, {"SQLite"}, {"SQL Server", "sql server", "mssql"},
            {"Docker"}, {"Kubernetes", "kubernetes", "k8s"}, {"AWS", "aws", "amazon web services"},
            {"Azure"}, {"GCP", "gcp", "google cloud"}, {"Terraform"}, {"Ansible"}, {"Jenkins"},
            {"GitLab CI"}, {"GitHub Actions"}, {"CI/CD", "ci/cd", "cicd"}, {"Git"}, {"Linux"},
            {"Maven"}, {"Gradle"}, {"JUnit"}, {"Mockito"}, {"Selenium"},
            {"Machine Learning"}, {"Deep Learning"}, {"TensorFlow"}, {"PyTorch"}, {"Pandas"}, {"NumPy"},
            {"Scikit-learn", "scikit-learn", "sklearn"}, {"Spark", "spark", "pyspark"}, {"Hadoop"},
            {"Tableau"}, {"Power BI"}, {"Figma"}, {"Agile"}, {"Scrum"}, {"Jira"},
            {"Android"}, {"iOS"}, {"Flutter"}
    });

    private final Clock clock;

    public CandidateDetailsExtractor() {
        this(Clock.systemDefaultZone());
    }

    CandidateDetailsExtractor(Clock clock) {
        this.clock = clock;
    }

    public CandidateDetailsDto extract(String text) {

        if (text == null || text.isBlank()) {
            return CandidateDetailsDto.empty();
        }

        List<String> lines = text.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();

        List<String> preferenceLines = lines.stream()
                .filter(line -> PREFERENCE_LINE.matcher(line).find())
                .toList();

        return new CandidateDetailsDto(
                extractFullName(text, lines),
                extractEmail(text),
                extractPhone(text),
                extractYearsOfExperience(text, lines),
                extractSkills(text),
                extractLocation(text),
                findFirstMatch(JOB_TYPE_PATTERNS, preferenceLines, text),
                findFirstMatch(LOCATION_TYPE_PATTERNS, preferenceLines, text)
        );
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
        while (matcher.find()) {
            long digits = matcher.group().chars().filter(Character::isDigit).count();
            if (digits >= MIN_PHONE_DIGITS && digits <= MAX_PHONE_DIGITS) {
                return matcher.group().trim();
            }
        }

        return null;
    }

    String extractFullName(String text, List<String> lines) {
        Matcher labelMatcher = NAME_LABEL_PATTERN.matcher(text);
        if (labelMatcher.find()) {
            return truncate(labelMatcher.group(1).strip());
        }

        return lines.stream()
                .limit(NAME_SEARCH_LINES)
                .filter(line -> NAME_PATTERN.matcher(line).matches())
                .filter(line -> Arrays.stream(line.toLowerCase(Locale.ROOT).split("\\s+"))
                        .noneMatch(NAME_STOP_WORDS::contains))
                .findFirst()
                .map(CandidateDetailsExtractor::normalizeCapitalization)
                .orElse(null);
    }

    Integer extractYearsOfExperience(String text, List<String> lines) {
        // 1. The candidate states it explicitly: trust the highest number mentioned
        int explicitYears = -1;
        for (Pattern pattern : EXPLICIT_YEARS_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int years = Integer.parseInt(matcher.group(1));
                if (years <= MAX_YEARS) {
                    explicitYears = Math.max(explicitYears, years);
                }
            }
        }
        if (explicitYears >= 0) {
            return explicitYears;
        }

        // 2. Otherwise add up the employment date ranges (overlaps merged),
        //    looking only at the "Experience" section when one can be found
        String experienceText = findExperienceSection(lines).orElseGet(() -> lines.stream()
                .filter(line -> !EDUCATION_LINE.matcher(line).find())
                .collect(Collectors.joining("\n")));
        List<YearMonth[]> ranges = extractDateRanges(experienceText);
        if (ranges.isEmpty()) {
            return null;
        }

        long months = totalMonthsWithoutOverlap(ranges);
        return (int) Math.min(MAX_YEARS, months / 12);
    }

    private Optional<String> findExperienceSection(List<String> lines) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (start < 0 && EXPERIENCE_HEADING.matcher(lines.get(i)).matches()) {
                start = i + 1;
            } else if (start >= 0 && OTHER_HEADING.matcher(lines.get(i)).matches()) {
                return Optional.of(String.join("\n", lines.subList(start, i)));
            }
        }
        return start >= 0 ? Optional.of(String.join("\n", lines.subList(start, lines.size()))) : Optional.empty();
    }

    private List<YearMonth[]> extractDateRanges(String text) {
        YearMonth now = YearMonth.now(clock);
        List<YearMonth[]> ranges = new ArrayList<>();

        Matcher matcher = DATE_RANGE_PATTERN.matcher(text);
        while (matcher.find()) {
            YearMonth start = YearMonth.of(
                    Integer.parseInt(matcher.group(3)),
                    parseMonth(matcher.group(1), matcher.group(2), 1)
            );

            YearMonth end = matcher.group(7) != null
                    ? now
                    : YearMonth.of(
                            Integer.parseInt(matcher.group(6)),
                            // without a month "2018 - 2020" counts as 2 years
                            parseMonth(matcher.group(4), matcher.group(5), start.getMonthValue())
                    );

            if (!end.isBefore(start) && !start.isAfter(now)) {
                ranges.add(new YearMonth[]{start, end.isAfter(now) ? now : end});
            }
        }
        return ranges;
    }

    private static int parseMonth(String monthName, String monthNumber, int defaultMonth) {
        if (monthName != null) {
            String prefix = monthName.substring(0, 3).toLowerCase(Locale.ROOT);
            return List.of("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                    .indexOf(prefix) + 1;
        }
        if (monthNumber != null) {
            int month = Integer.parseInt(monthNumber);
            if (month >= 1 && month <= 12) {
                return month;
            }
        }
        return defaultMonth;
    }

    private static long totalMonthsWithoutOverlap(List<YearMonth[]> ranges) {
        List<YearMonth[]> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(range -> range[0]));

        long months = 0;
        YearMonth currentStart = sorted.getFirst()[0];
        YearMonth currentEnd = sorted.getFirst()[1];

        for (YearMonth[] range : sorted.subList(1, sorted.size())) {
            if (range[0].isAfter(currentEnd)) {
                months += currentStart.until(currentEnd, ChronoUnit.MONTHS);
                currentStart = range[0];
                currentEnd = range[1];
            } else if (range[1].isAfter(currentEnd)) {
                currentEnd = range[1];
            }
        }
        return months + currentStart.until(currentEnd, ChronoUnit.MONTHS);
    }

    Set<String> extractSkills(String text) {
        Set<String> skills = new LinkedHashSet<>();
        SKILL_PATTERNS.forEach((skill, pattern) -> {
            if (pattern.matcher(text).find()) {
                skills.add(skill);
            }
        });
        return skills;
    }

    String extractLocation(String text) {
        Matcher labelMatcher = LOCATION_LABEL_PATTERN.matcher(text);
        if (labelMatcher.find()) {
            return cleanLocation(labelMatcher.group(1));
        }

        Matcher basedInMatcher = BASED_IN_PATTERN.matcher(text);
        if (basedInMatcher.find()) {
            return cleanLocation(basedInMatcher.group(1));
        }

        return null;
    }

    /**
     * Lines that talk about preferences ("Open to remote work") win over
     * incidental mentions elsewhere in the CV ("worked remotely for ...").
     */
    private static <T> T findFirstMatch(Map<T, Pattern> patterns, List<String> preferenceLines, String text) {
        for (String line : preferenceLines) {
            for (Map.Entry<T, Pattern> entry : patterns.entrySet()) {
                if (entry.getValue().matcher(line).find()) {
                    return entry.getKey();
                }
            }
        }
        for (Map.Entry<T, Pattern> entry : patterns.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Map<String, Pattern> buildSkillPatterns(String[][] definitions) {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        for (String[] definition : definitions) {
            String canonical = definition[0];
            List<String> aliases = definition.length > 1
                    ? Arrays.asList(definition).subList(1, definition.length)
                    : List.of(canonical);

            String alternatives = aliases.stream()
                    .map(Pattern::quote)
                    .reduce((a, b) -> a + "|" + b)
                    .orElseThrow();

            // custom boundaries so that "Java" does not match "JavaScript" and "C++" / ".NET" still work
            patterns.put(canonical, Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + alternatives + ")(?![\\p{L}\\p{N}+#])", FLAGS));
        }
        return patterns;
    }

    private static String cleanLocation(String value) {
        String location = value.split("[|•·]")[0].strip()
                .replaceAll("[.,;:\\s]+$", "");
        return location.isEmpty() ? null : truncate(location);
    }

    private static String normalizeCapitalization(String name) {
        if (!name.equals(name.toUpperCase(Locale.ROOT))) {
            return name;
        }
        StringBuilder builder = new StringBuilder();
        for (String part : name.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static String truncate(String value) {
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
