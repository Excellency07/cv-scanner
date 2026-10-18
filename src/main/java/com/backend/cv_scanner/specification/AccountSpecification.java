package com.backend.cv_scanner.specification;

import com.backend.cv_scanner.dao.entity.Account;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AccountSpecification {
    public static Specification<Account> getAccountSpecification(
            String keyword,
            Instant dateFrom,
            Instant dateTo
    ) {
        return new Specification<Account>() {
            @Override
            public @Nullable Predicate toPredicate(@Nonnull Root<Account> root, @Nonnull CriteriaQuery<?> query, @Nonnull CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();

                if (keyword != null) {
                    Predicate predicateName = criteriaBuilder.like(root.get("name"), "%" + keyword + "%");
                    Predicate predicateEmail = criteriaBuilder.like(root.get("email"), "%" + keyword + "%");

                    predicates.add(criteriaBuilder.or(predicateName, predicateEmail));
                }

                if (dateFrom != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
                }

                if (dateTo != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), dateTo));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }
}
