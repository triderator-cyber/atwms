package com.atwms.common.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Eine Einschraenkung fuer Abfragen auf {@code T}.
 *
 * <p>Als funktionales Interface ausgelegt, damit Bedingungen als Lambda
 * uebergeben und - anders als bei JPQL-Zeichenketten - als Methode
 * wiederverwendet oder kombiniert werden koennen.</p>
 */
@FunctionalInterface
public interface Restriction<T> {

    Predicate toPredicate(CriteriaBuilder cb, Root<T> root);

    /** Verknuepft zwei Einschraenkungen mit UND. */
    default Restriction<T> and(Restriction<T> other) {
        return (cb, root) -> cb.and(this.toPredicate(cb, root), other.toPredicate(cb, root));
    }

    /** Verknuepft zwei Einschraenkungen mit ODER. */
    default Restriction<T> or(Restriction<T> other) {
        return (cb, root) -> cb.or(this.toPredicate(cb, root), other.toPredicate(cb, root));
    }
}
