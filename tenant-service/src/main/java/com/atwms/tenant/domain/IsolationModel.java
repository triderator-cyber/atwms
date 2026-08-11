package com.atwms.tenant.domain;

/**
 * Datenisolationsstufe eines Mandanten (Tenant-Tiering).
 *
 * <p>Die Plattform startet fuer alle Mandanten mit {@link #POOLED}. Einzelne
 * Grosskunden koennen spaeter auf {@link #SILO} migriert werden, ohne dass sich
 * die Architektur der Services aendert - lediglich die DataSource-Aufloesung
 * unterscheidet sich.</p>
 */
public enum IsolationModel {

    /** Gemeinsames Schema, Trennung ueber die Spalte tenant_id + Row Level Security. */
    POOLED,

    /** Eigene Datenbank bzw. eigenes Schema pro Mandant. */
    SILO
}
