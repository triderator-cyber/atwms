package com.atwms.billing.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * Definiert die DataSource des Billing-Service.
 *
 * <p>Bewusst per {@code @DataSourceDefinition} statt ueber einen
 * provisionierten WildFly-Layer: die Annotation gehoert zum Jakarta-EE-Standard,
 * der JDBC-Treiber liegt als normale Abhaengigkeit im WAR, und damit laeuft das
 * Deployment auf jedem Jakarta-EE-Server ohne serverspezifische
 * Vorkonfiguration.</p>
 *
 * <p>Fuer Produktion gehoeren Host, Benutzer und Passwort nicht in den Code.
 * Dort ersetzt man diese Klasse durch eine serverseitig verwaltete DataSource
 * (WildFly-CLI oder Datasources-Feature-Pack) und traegt in der
 * {@code persistence.xml} deren JNDI-Namen ein - am Rest der Anwendung
 * aendert sich nichts.</p>
 */
@Singleton
@Startup
@DataSourceDefinition(
        name = "java:app/jdbc/BillingDS",
        className = "org.postgresql.ds.PGSimpleDataSource",
        serverName = "localhost",
        portNumber = 5433,
        databaseName = "billingdb",
        user = "atwms",
        password = "atwms",
        transactional = true,
        minPoolSize = 2,
        maxPoolSize = 20)
public class DataSourceConfig {
}
