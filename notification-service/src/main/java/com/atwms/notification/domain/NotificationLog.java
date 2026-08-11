package com.atwms.notification.domain;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurzzeitgedaechtnis der zuletzt versendeten Benachrichtigungen, damit sich
 * der Event-Fluss lokal ohne Mail-Server nachvollziehen laesst.
 *
 * <p><strong>Nur fuer die Entwicklung.</strong> Ein Zustand im Speicher
 * verhindert echtes horizontales Skalieren - in Produktion gehoert der
 * Versandverlauf in eine Datenbank oder in ein eigenes Topic.</p>
 */
@ApplicationScoped
public class NotificationLog {

    private static final int MAX_PER_TENANT = 50;

    private final Map<String, Deque<Notification>> byTenant = new ConcurrentHashMap<>();

    public void record(Notification notification) {
        Deque<Notification> entries = byTenant.computeIfAbsent(
                notification.getTenantId(), key -> new ArrayDeque<>());
        synchronized (entries) {
            entries.addFirst(notification);
            while (entries.size() > MAX_PER_TENANT) {
                entries.removeLast();
            }
        }
    }

    public List<Notification> recentFor(String tenantId) {
        Deque<Notification> entries = byTenant.get(tenantId);
        if (entries == null) {
            return List.of();
        }
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }
}
