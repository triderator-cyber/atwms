package com.atwms.common.tenant;

import com.atwms.common.security.SaasClaims;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.logging.Logger;

/**
 * Uebersetzt das von Keycloak ausgestellte JWT in einen {@link TenantContext}.
 *
 * <p>Laeuft unmittelbar nach der Authentifizierung und damit vor jeder
 * Resource-Methode. Ab diesem Punkt arbeitet die gesamte Anwendung nur noch mit
 * dem {@code TenantContext} und nie mehr direkt mit dem Token - das haelt die
 * Fachlogik frei von Security-Details und macht sie einfach testbar.</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class TenantResolutionFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TenantResolutionFilter.class.getName());

    @Inject
    Instance<JsonWebToken> jwt;

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (jwt.isUnsatisfied()) {
            return;
        }
        try {
            JsonWebToken token = jwt.get();
            if (token == null || token.getRawToken() == null) {
                return;
            }
            tenantContext.initialise(
                    claimAsString(token, SaasClaims.TENANT_ID),
                    claimAsString(token, SaasClaims.TENANT_TIER),
                    token.getSubject());
        } catch (RuntimeException e) {
            // Unauthentifizierter Aufruf (z. B. /health oder /openapi): je nach
            // Implementierung ist dann gar kein Token-Bean aufloesbar. Der
            // TenantContext bleibt leer - ein fachlicher Endpoint antwortet
            // spaetestens ueber requireTenantId() mit 403.
            LOG.fine(() -> "Kein Token im Request: " + e.getMessage());
        }
    }

    private static String claimAsString(JsonWebToken token, String claimName) {
        Object value = token.getClaim(claimName);
        if (value == null) {
            return null;
        }
        if (value instanceof JsonString jsonString) {
            return jsonString.getString();
        }
        return value.toString();
    }
}
