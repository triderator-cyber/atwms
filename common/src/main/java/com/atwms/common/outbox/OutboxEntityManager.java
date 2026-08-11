package com.atwms.common.outbox;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Kennzeichnet den EntityManager, ueber den die Outbox schreibt.
 *
 * <p>Das gemeinsame Modul kann keine Persistence Unit fest verdrahten - jeder
 * Service hat seine eigene ({@code orderPU}, {@code billingPU}, ...). Deshalb
 * liefert jeder Service seinen EntityManager unter diesem Qualifier, und die
 * Outbox greift nur noch darauf zu.</p>
 *
 * <p>Entscheidend fuer das Muster: Es ist derselbe EntityManager und damit
 * dieselbe Transaktion wie die der Fachlogik. Nur so entstehen fachliche
 * Aenderung und Event garantiert gemeinsam.</p>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface OutboxEntityManager {
}
