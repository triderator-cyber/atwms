package com.atwms.order.client;

import com.atwms.common.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Kapselt den Aufruf des Tenant-Service inklusive Resilienz-Verhalten.
 *
 * <p>Genau hier zeigt sich der Unterschied zum Monolithen: was frueher ein
 * lokaler Methodenaufruf war, ist jetzt ein Netzwerkaufruf, der langsam sein
 * oder ausfallen kann. MicroProfile Fault Tolerance macht dieses Verhalten
 * deklarativ:</p>
 *
 * <ul>
 *   <li>{@code @Timeout} - ein haengender Tenant-Service darf keine
 *       Order-Threads blockieren.</li>
 *   <li>{@code @Retry} - kurze Netzwerkaussetzer werden abgefangen.</li>
 *   <li>{@code @CircuitBreaker} - bei anhaltendem Ausfall wird nicht weiter
 *       gegen die Wand gelaufen, der Kreis oeffnet sich und die Aufrufe
 *       schlagen sofort fehl.</li>
 *   <li>{@code @Fallback} - definiertes Ersatzverhalten statt HTTP 500.</li>
 * </ul>
 */
@ApplicationScoped
public class TenantPolicy {

    private static final Logger LOG = Logger.getLogger(TenantPolicy.class.getName());
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final Map<String, CachedTenant> cache = new ConcurrentHashMap<>();

    @Inject
    @RestClient
    TenantClient tenantClient;

    @Inject
    TenantContext tenantContext;

    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, retryOn = RuntimeException.class)
    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 10_000, successThreshold = 2)
    @Fallback(fallbackMethod = "fromCache")
    public Optional<TenantInfo> currentTenant() {
        String tenantId = tenantContext.requireTenantId();
        TenantInfo info = tenantClient.byId(tenantId);
        cache.put(tenantId, new CachedTenant(info, Instant.now()));
        return Optional.of(info);
    }

    /**
     * Fallback: der zuletzt bekannte Zustand, sofern er nicht zu alt ist.
     *
     * <p>Bewusst "fail closed": liegt kein brauchbarer Cache-Eintrag vor, wird
     * ein leeres Optional zurueckgegeben und der Aufrufer lehnt den
     * Schreibzugriff ab. Bei einem abrechnungsrelevanten Limit ist es die
     * bessere Wahl, im Zweifel abzulehnen, statt einem gesperrten Mandanten
     * unbegrenzt Bestellungen zu erlauben.</p>
     */
    @SuppressWarnings("unused")
    Optional<TenantInfo> fromCache() {
        String tenantId = tenantContext.getTenantId().orElse(null);
        if (tenantId == null) {
            return Optional.empty();
        }
        CachedTenant cached = cache.get(tenantId);
        if (cached == null || cached.isOlderThan(CACHE_TTL)) {
            LOG.warning("Tenant-Service nicht erreichbar und kein gueltiger Cache-Eintrag fuer '"
                    + tenantId + "' - Schreibzugriff wird abgelehnt.");
            return Optional.empty();
        }
        LOG.info("Tenant-Service nicht erreichbar - verwende zwischengespeicherten Stand fuer '"
                + tenantId + "'.");
        return Optional.of(cached.info());
    }

    private record CachedTenant(TenantInfo info, Instant fetchedAt) {

        boolean isOlderThan(Duration ttl) {
            return fetchedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}
