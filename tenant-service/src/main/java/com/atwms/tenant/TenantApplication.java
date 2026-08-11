package com.atwms.tenant;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;

/**
 * JAX-RS-Aktivierung des Tenant-Service.
 *
 * <p>{@code @LoginConfig(authMethod = "MP-JWT")} schaltet die Bearer-Token-
 * Validierung nach MicroProfile JWT ein: der Service prueft eingehende Tokens
 * eigenstaendig gegen den JWKS-Endpoint von Keycloak und haelt selbst keine
 * Session. Genau das macht ihn horizontal skalierbar.</p>
 */
@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT", realmName = "saas")
public class TenantApplication extends Application {
}
