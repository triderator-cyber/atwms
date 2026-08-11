package com.atwms.common.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

/**
 * Basis fuer die Datenbankzugriffsklasse eines Service.
 *
 * <p>Jeder Service besitzt genau eine abgeleitete Klasse, in der saemtliche
 * Abfragen dieses Service liegen. Diese Basis kapselt den immer gleichen
 * Dreiklang aus {@code CriteriaBuilder}, {@code CriteriaQuery} und
 * {@code Root}, damit in den Services nur noch die fachliche Bedingung
 * uebrig bleibt.</p>
 *
 * <p>Die Methoden sind ueber den Entity-Typ parametrisiert und nicht die
 * Klasse selbst - eine Zugriffsklasse bedient damit alle Entities ihres
 * Service, nicht nur eine.</p>
 *
 * <p>Bewusst ohne CDI-Annotationen und ohne eigene Injektionspunkte: Die
 * Klasse liegt in {@code common} und darf den Services nichts aufzwingen. Den
 * EntityManager liefert jede Ableitung selbst - nur sie kennt ihre Persistence
 * Unit.</p>
 */
public abstract class DatabaseAccess {

    /** Der EntityManager der Persistence Unit dieses Service. */
    protected abstract EntityManager em();

    protected CriteriaBuilder cb() {
        return em().getCriteriaBuilder();
    }

    /**
     * Baut eine Abfrage und gibt sie unausgefuehrt zurueck - fuer Faelle, die
     * zusaetzlich Sperren, Obergrenzen oder Hinweise brauchen.
     */
    protected <T> TypedQuery<T> select(Class<T> type, Restriction<T> where, Ordering<T> order) {
        CriteriaBuilder cb = cb();
        CriteriaQuery<T> query = cb.createQuery(type);
        Root<T> root = query.from(type);
        query.select(root);
        if (where != null) {
            query.where(where.toPredicate(cb, root));
        }
        if (order != null) {
            query.orderBy(order.toOrders(cb, root));
        }
        return em().createQuery(query);
    }

    protected <T> List<T> list(Class<T> type, Restriction<T> where, Ordering<T> order) {
        return select(type, where, order).getResultList();
    }

    protected <T> List<T> list(Class<T> type, Restriction<T> where) {
        return list(type, where, null);
    }

    /** Zugriff ueber den Primaerschluessel. */
    protected <T> Optional<T> byKey(Class<T> type, Object key) {
        return Optional.ofNullable(em().find(type, key));
    }

    protected <T> long count(Class<T> type, Restriction<T> where) {
        CriteriaBuilder cb = cb();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(type);
        query.select(cb.count(root));
        if (where != null) {
            query.where(where.toPredicate(cb, root));
        }
        return em().createQuery(query).getSingleResult();
    }

    protected <T> boolean exists(Class<T> type, Restriction<T> where) {
        return count(type, where) > 0;
    }

    /** Aggregat, etwa {@code SUM} oder {@code MAX}, mit eigenem Ergebnistyp. */
    protected <T, R> R aggregate(Class<T> type, Class<R> resultType,
                                 Projection<T, R> projection, Restriction<T> where) {
        CriteriaBuilder cb = cb();
        CriteriaQuery<R> query = cb.createQuery(resultType);
        Root<T> root = query.from(type);
        query.select(projection.toExpression(cb, root));
        if (where != null) {
            query.where(where.toPredicate(cb, root));
        }
        return em().createQuery(query).getSingleResult();
    }

    protected <T> int deleteWhere(Class<T> type, Restriction<T> where) {
        CriteriaBuilder cb = cb();
        CriteriaDelete<T> delete = cb.createCriteriaDelete(type);
        Root<T> root = delete.from(type);
        if (where != null) {
            delete.where(where.toPredicate(cb, root));
        }
        return em().createQuery(delete).executeUpdate();
    }

    protected <E> E persist(E entity) {
        em().persist(entity);
        return entity;
    }
}
