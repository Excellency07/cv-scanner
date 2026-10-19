package com.backend.cv_scanner.specification;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.model.dto.candidate.CandidateFilterDto;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CandidateSpecification {
    public static Specification<Candidate> getCandidateSpecification(CandidateFilterDto filter) {
        return new Specification<Candidate>() {
            @Override
            public @Nullable Predicate toPredicate(@Nonnull Root<Candidate> root, @Nonnull CriteriaQuery<?> query, @Nonnull CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();

                if (hasText(filter.keyword())) {
                    String pattern = likePattern(filter.keyword());
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)
                    ));
                }

                // every requested skill must be present ("Java AND Spring Boot"), case-insensitive
                if (filter.skills() != null) {
                    filter.skills().stream()
                            .filter(CandidateSpecification::hasText)
                            .forEach(skill -> predicates.add(hasSkill(root, query, criteriaBuilder, skill)));
                }

                if (filter.minExperience() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("yearsOfExperience"), filter.minExperience()));
                }

                if (filter.maxExperience() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("yearsOfExperience"), filter.maxExperience()));
                }

                if (hasText(filter.location())) {
                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), likePattern(filter.location())));
                }

                if (filter.jobType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("jobType"), filter.jobType()));
                }

                if (filter.jobLocationType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("jobLocationType"), filter.jobLocationType()));
                }

                if (filter.batchId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("file").get("batch").get("id"), filter.batchId()));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    private static Predicate hasSkill(Root<Candidate> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder, String skill) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Candidate> candidate = subquery.from(Candidate.class);
        Join<Candidate, String> skills = candidate.join("skills");

        subquery.select(criteriaBuilder.literal(1))
                .where(
                        criteriaBuilder.equal(candidate.get("id"), root.get("id")),
                        criteriaBuilder.equal(criteriaBuilder.lower(skills), skill.strip().toLowerCase(Locale.ROOT))
                );

        return criteriaBuilder.exists(subquery);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String likePattern(String value) {
        return "%" + value.strip().toLowerCase(Locale.ROOT) + "%";
    }
}
