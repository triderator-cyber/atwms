package com.atwms.tenant.domain;

/**
 * Lebenszyklus eines Mandanten. Nur {@link #ACTIVE} erlaubt fachliche Schreibzugriffe.
 */
public enum TenantStatus {

    /** Angelegt, Provisionierung laeuft noch (Schema, Keycloak-Gruppe, ...). */
    PROVISIONING,

    /** Regulaer nutzbar. */
    ACTIVE,

    /** Z. B. wegen offener Rechnung gesperrt - Lesezugriff ja, Schreibzugriff nein. */
    SUSPENDED,

    /** Gekuendigt, Daten stehen zur Loeschung an. */
    TERMINATED
}
