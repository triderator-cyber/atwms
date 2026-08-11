package com.atwms.common.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.List;

/**
 * Eine Sortierung fuer Abfragen auf {@code T}.
 *
 * <p>Bewusst ohne Bequemlichkeitsmethoden mit Attributnamen als Zeichenkette:
 * Sortierungen werden ueber das Metamodell ausgedrueckt
 * ({@code (cb, root) -> List.of(cb.desc(root.get(Tenant_.createdAt)))}), damit
 * ein Tippfehler beim Uebersetzen auffaellt und nicht erst zur Laufzeit.</p>
 */
@FunctionalInterface
public interface Ordering<T> {

    List<Order> toOrders(CriteriaBuilder cb, Root<T> root);
}
