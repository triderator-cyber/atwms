package com.atwms.common.events;

/**
 * Zentrale Definition der Kafka-Topics.
 *
 * <p>Die Namen stehen bewusst im gemeinsamen Modul: Producer und Consumer
 * teilen sich damit garantiert dieselbe Konstante, statt den Topic-Namen an
 * mehreren Stellen als String zu wiederholen.</p>
 */
public final class Topics {

    /** Wird vom Order-Service publiziert, von Billing und Notification konsumiert. */
    public static final String ORDER_CREATED = "order-created";

    private Topics() {
    }
}
