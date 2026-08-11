package com.atwms.common.security;

/**
 * Namen der von Keycloak ausgestellten Custom-Claims.
 *
 * <p>Der Claim {@code tenant_id} wird in Keycloak ueber einen Protocol-Mapper
 * vom Typ "User Attribute" befuellt (siehe docker/keycloak/realm-saas.json).</p>
 */
public final class SaasClaims {

    /** Mandantenkennung, die den kompletten Request-Kontext bestimmt. */
    public static final String TENANT_ID = "tenant_id";

    /** Abrechnungsstufe des Mandanten, z. B. "free", "pro", "enterprise". */
    public static final String TENANT_TIER = "tenant_tier";

    private SaasClaims() {
    }
}
