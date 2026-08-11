package com.atwms.common.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

/**
 * Ein Ergebnisausdruck fuer Aggregate wie SUM, MIN oder MAX.
 *
 * @param <T> Entity, ueber die aggregiert wird
 * @param <R> Ergebnistyp des Ausdrucks
 */
@FunctionalInterface
public interface Projection<T, R> {

    Expression<R> toExpression(CriteriaBuilder cb, Root<T> root);
}
