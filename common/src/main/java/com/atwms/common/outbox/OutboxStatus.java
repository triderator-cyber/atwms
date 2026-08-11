package com.atwms.common.outbox;

/**
 * Zustand eines Outbox-Eintrags.
 */
public enum OutboxStatus {

    /** Geschrieben, aber noch nicht nach Kafka uebertragen. */
    NEW,

    /** Von Kafka bestaetigt. Bleibt zu Nachvollziehbarkeit noch eine Weile stehen. */
    SENT,

    /** Nach mehreren Versuchen endgueltig gescheitert - braucht manuelle Klaerung. */
    FAILED
}
