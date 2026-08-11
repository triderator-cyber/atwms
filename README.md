# SaaS-Plattform auf Jakarta EE, MicroProfile und Keycloak

Grundgeruest fuer eine mandantenfaehige SaaS-Anwendung, aufgeteilt in vier
unabhaengig deploybare Microservices. Jeder Service ist eine eigenstaendige
Jakarta-EE-10-Anwendung, laeuft in einem eigenen WildFly-Bootable-JAR und
besitzt seine eigene Datenbank. Die Mandantentrennung haengt durchgaengig am
JWT von Keycloak.

## Ueberblick

Der **Tenant-Service** ist die Control Plane: er verwaltet, welche Mandanten
existieren, in welchem Status sie sind und welche Limits fuer sie gelten. Der
**Order-Service** enthaelt die eigentliche Fachlogik und ist der einzige
Service, der Bestellungen schreibt. Der **Billing-Service** fuehrt den
Verbrauch pro Mandant und Abrechnungszeitraum. Der **Notification-Service**
verschickt Benachrichtigungen. Dazu kommen Keycloak als Identity Provider,
PostgreSQL als Datenbank und Kafka als Event-Bus.

Zwei Kommunikationswege werden bewusst unterschieden. Wo eine Entscheidung
sofort gebraucht wird, ruft ein Service synchron per REST auf: der
Order-Service fragt beim Tenant-Service nach, ob der Mandant aktiv ist und sein
Limit noch nicht ausgeschoepft hat. Wo andere Services lediglich informiert
werden muessen, geht ein Event nach Kafka: nach dem Anlegen einer Bestellung
veroeffentlicht der Order-Service `OrderCreated`, und Billing- sowie
Notification-Service konsumieren es unabhaengig voneinander in getrennten
Consumer-Groups. Der Order-Service kennt die beiden Konsumenten nicht - ein
fuenfter Service koennte das Topic abonnieren, ohne dass am Order-Service eine
Zeile geaendert wird.

```
                          Keycloak (8180)
                                |  JWT mit tenant_id, groups, aud
                                v
   ┌────────────────┐   REST   ┌────────────────┐
   │ Order-Service  │ ───────▶ │ Tenant-Service │   Control Plane
   │     (8082)     │          │     (8081)     │
   └───────┬────────┘          └────────────────┘
           │ OrderCreated
           v
       ┌───────┐        ┌──────────────────┐
       │ Kafka │ ─────▶ │ Billing (8083)   │
       │ 9092  │ ─────▶ │ Notification(8084)│
       └───────┘        └──────────────────┘

   orderdb        tenantdb        billingdb      (getrennte Datenbanken)
```

## Mandantenfaehigkeit

Umgesetzt ist das **Pooled-Modell**: alle Mandanten teilen sich eine Datenbank,
getrennt wird ueber die Spalte `tenant_id`. Das ist im Betrieb am guenstigsten
und skaliert am einfachsten. Fuer Grosskunden mit Isolationsanforderungen ist
in `IsolationModel` bereits `SILO` vorgesehen - dann bekommt der Mandant ein
eigenes Schema oder eine eigene Datenbank, ohne dass sich die Servicearchitektur
aendert.

Die Trennung ist in drei Ebenen abgesichert. Zuerst kommt die Mandantenkennung
ausschliesslich aus dem JWT: `TenantResolutionFilter` liest den Claim
`tenant_id` und legt ihn in den request-scoped `TenantContext`. In keinem
einzigen REST-Endpoint taucht eine Mandanten-ID als Parameter auf, ein Client
kann also gar nicht erst versuchen, fremde Daten anzufordern. Danach setzt die
Persistenzschicht nach: `TenantAwareEntity` fuellt `tenant_id` beim Schreiben
automatisch, und alle Lesezugriffe laufen ueber NamedQueries, die den Mandanten
als Pflichtparameter fuehren. Als dritte Ebene liegt in
`docker/postgres/init.sql` eine vorbereitete Row-Level-Security-Policy bereit;
sie ist auskommentiert, weil sie erst funktioniert, wenn die Anwendung
`SET LOCAL app.tenant_id` pro Transaktion setzt.

## Keycloak

Der Realm `saas` wird beim Start automatisch importiert. Er enthaelt die Rollen
`platform-admin`, `tenant-admin` und `user`, drei Testbenutzer sowie den
Public Client `saas-frontend`.

Vier Protocol-Mapper sind dabei entscheidend. Der Mapper `tenant-id` schreibt
das Benutzerattribut `tenant_id` als Claim ins Access Token - das ist der
Anker der gesamten Mandantentrennung. `tenant-tier` transportiert den Tarif.
`realm-roles-as-groups` bildet die Realm-Rollen auf den Claim `groups` ab:
Keycloak legt Rollen normalerweise unter `realm_access.roles` ab, MicroProfile
JWT erwartet sie aber unter `groups`, sonst greift kein einziges
`@RolesAllowed`. Der Mapper `saas-api-audience` setzt schliesslich die Audience,
die die Services ueber `mp.jwt.verify.audiences` pruefen.

Testbenutzer: `platformadmin` / `admin` (Rolle platform-admin), `alice` /
`alice` (Mandant acme, tenant-admin) und `bob` / `bob` (Mandant globex, user).
Bob dient dazu, die Mandantentrennung gegenzupruefen - er darf die Daten von
acme nicht sehen, obwohl sie in derselben Tabelle liegen.

## Lokal starten

Voraussetzungen sind JDK 17 oder neuer, Maven 3.9 und Docker.

Zuerst die Infrastruktur hochfahren:

```bash
docker compose up -d
```

Keycloak braucht beim ersten Start etwa eine halbe Minute, bis der Realm
importiert ist. Danach die Services bauen:

```bash
mvn clean install -Pbootable-jar
```

Das Profil `bootable-jar` provisioniert per Galleon einen minimalen
WildFly-Server mit genau den benoetigten Layern und packt ihn zusammen mit der
Anwendung in ein selbst lauffaehiges JAR. Der erste Lauf laedt die
Feature-Packs herunter und dauert entsprechend laenger.

Anschliessend jeden Service starten, jeweils mit eigenem Port-Offset:

```bash
java -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
     -Djboss.socket.binding.port-offset=1 \
     -jar tenant-service/target/tenant-service-bootable.jar
```

Analog fuer die uebrigen Services mit Offset 2, 3 und 4. Die Datenbankverbindung
steht in der jeweiligen Klasse `DataSourceConfig` und braucht keine
Umgebungsvariablen. In IntelliJ ist das bereits als Run-Konfiguration
hinterlegt, siehe unten.

| Komponente | Port | Datenbank |
|---|---|---|
| Keycloak | 8180 | - |
| PostgreSQL | 5433 | - |
| Kafka | 9092 | - |
| Tenant-Service | 8081 | tenantdb |
| Order-Service | 8082 | orderdb |
| Billing-Service | 8083 | billingdb |
| Notification-Service | 8084 | - |

Jeder Service stellt zusaetzlich `/health/live`, `/health/ready` und `/openapi`
bereit. Metriken sind bewusst noch nicht aktiviert: MicroProfile Metrics wurde
in WildFly durch Micrometer ersetzt. Wer sie braucht, ergaenzt im
`bootable-jar`-Profil den Layer `micrometer` und verwendet die Annotationen aus
`io.micrometer`.

## In IntelliJ

Das Projekt ueber *File → Open* am Wurzel-`pom.xml` oeffnen; IntelliJ erkennt
das Multi-Module-Projekt und importiert alle fuenf Module. Unter *Project
Structure → Project SDK* ein JDK 17+ auswaehlen.

Im Ordner `.run` liegen fertige, eingecheckte Run-Konfigurationen: eine fuer
den Gesamtbuild und je eine pro Service, inklusive Port-Offset und
Datenbank-Umgebungsvariablen. Sie erscheinen nach dem Import automatisch in der
Auswahlliste oben rechts. Reihenfolge: erst `docker compose up -d`, dann
*00 Build*, danach die vier Services in beliebiger Reihenfolge.

Die Datei `requests.http` enthaelt einen kompletten Ende-zu-Ende-Durchlauf fuer
den HTTP-Client von IntelliJ: Token holen, Mandanten anlegen und aktivieren,
Bestellung erfassen, Verbrauch und Benachrichtigung pruefen und zum Schluss mit
Bobs Token gegentesten, dass die Mandantentrennung greift.

## Aufbau der Module

Das Modul `common` enthaelt alles, was alle Services teilen: den
`TenantContext`, den JWT-Filter, die Basisklasse fuer mandantenbezogene
Entities, das einheitliche Fehlerformat und die Event-Typen samt Topic-Namen.
Bewusst enthaelt es **keine** Fachlogik - ein gemeinsames Modul mit
Geschaeftsregeln waere der schnellste Weg zurueck zum verteilten Monolithen,
weil dann jede Aenderung wieder alle Services gleichzeitig betrifft.

Die vier Servicemodule sind jeweils gleich geschnitten: `api` fuer
JAX-RS-Ressourcen und DTOs, `domain` fuer Entities, Repositories und
Fachlogik, `client` fuer ausgehende REST-Aufrufe, `messaging` fuer Kafka.
Nach aussen gehen ausschliesslich DTOs, nie Entities - die REST-Schnittstelle
ist ein Vertrag und soll sich nicht automatisch mitaendern, wenn das
Datenmodell angepasst wird.

## Resilienz

`TenantPolicy` im Order-Service zeigt exemplarisch, was ein Netzwerkaufruf
gegenueber einem lokalen Methodenaufruf zusaetzlich braucht: `@Timeout`, damit
ein haengender Tenant-Service keine Order-Threads blockiert, `@Retry` gegen
kurze Aussetzer, `@CircuitBreaker` gegen anhaltende Ausfaelle und `@Fallback`
fuer definiertes Ersatzverhalten. Der Fallback greift auf einen kurzlebigen
Cache zurueck und lehnt im Zweifel ab - bei einem abrechnungsrelevanten Limit
ist "fail closed" die richtige Wahl.

Auf der Consumer-Seite sorgt die Unique Constraint auf `source_event_id` im
Billing-Service fuer Idempotenz. Kafka garantiert "at least once", eine
Nachricht kann also mehrfach ankommen; ohne diese Absicherung wuerde derselbe
Vorgang mehrfach abgerechnet.

## Bewusst offen gelassen

Ein API-Gateway fehlt: aktuell spricht der Client die Services direkt an. In
Produktion gehoert davor ein Gateway fuer Routing, Rate-Limiting pro Mandant
und einheitliche Token-Pruefung.

Die Event-Veroeffentlichung ist noch nicht transaktionssicher. Faellt der
Order-Service zwischen Commit und Kafka-Send aus, geht das Event verloren. Der
uebliche naechste Schritt ist das Transactional-Outbox-Pattern: das Event wird
in derselben Transaktion in eine Outbox-Tabelle geschrieben und von dort
zuverlaessig nach Kafka uebertragen.

Das Schema wird zur Entwicklungszeit von Hibernate erzeugt
(`hibernate.hbm2ddl.auto=update`). Fuer Produktion gehoert das auf `none`,
zusammen mit Flyway oder Liquibase pro Service.

Ebenfalls offen: Container-Images und Kubernetes-Manifeste, verteiltes Tracing
mit OpenTelemetry, Integrationstests mit Testcontainers und Arquillian sowie
das eigentliche Provisioning beim Onboarding (Keycloak-Gruppe anlegen, Schema
erzeugen, Willkommensmail).

## Stolpersteine beim ersten Aufsetzen

Fuenf Dinge kosten erfahrungsgemaess Zeit, wenn man sie nicht kennt.

Der Start eines Bootable JARs braucht zwingend
`-Djava.util.logging.manager=org.jboss.logmanager.LogManager` als JVM-Option,
und zwar vor `-jar`. Fehlt sie, bootet WildFly scheinbar, beendet sich aber
nach wenigen Sekunden mit einer `IllegalStateException` zum LogManager. Ein
verlaesslicher Indikator ist das Logformat: erscheinen Zeilen im Stil
`Aug. 10, 2026 5:47:49 PM`, ist die Option nicht aktiv; korrekt ist
`17:47:49,123 INFO [org.jboss...]`.

Der Port-Offset funktioniert nur mit der Standardkonfiguration. Der Layer
`cloud-server` erzeugt eine containerorientierte Konfiguration ohne den
Ausdruck `${jboss.socket.binding.port-offset}` und bindet fest auf
`0.0.0.0:8080` - vier parallel laufende Services kollidieren dann alle auf
demselben Port. Deshalb wird dieser Layer hier bewusst nicht verwendet.

Die Datenbanken entstehen nur beim allerersten Start des PostgreSQL-Containers.
Skripte aus `/docker-entrypoint-initdb.d` laufen ausschliesslich, solange das
Datenverzeichnis leer ist. Wurde das Volume schon einmal angelegt, wird
`init.sql` stillschweigend uebersprungen. Abhilfe schafft entweder
`docker compose down -v` oder das nachtraegliche Anlegen von Hand:

```bash
docker exec saas-postgres psql -U atwms -d postgres -c "CREATE DATABASE tenantdb OWNER atwms;"
```

Ein zweites, nativ installiertes PostgreSQL auf demselben Rechner ist eine
heimtueckische Fehlerquelle: Unter Windows koennen sich beide Instanzen einen
Port teilen, und die Anwendung landet dann bei der falschen. Symptom ist eine
Fehlermeldung wie "Datenbank existiert nicht", obwohl sie im Container
nachweislich vorhanden ist. Ein guter Indikator ist die Sprache der Meldung -
der Container laeuft mit `en_US.utf8` und antwortet immer auf Englisch. Deshalb
belegt der Container hier Port 5433 statt 5432.

Adressen stehen bewusst als `127.0.0.1` und nicht als `localhost` in den
Konfigurationsdateien. WildFly bindet standardmaessig nur an den
IPv4-Loopback; loest ein Client `localhost` zu `::1` auf, laeuft der Aufruf
mit "Connection refused" ins Leere.

MicroProfile Metrics gibt es in aktuellen WildFly-Versionen nicht mehr, weder
als Layer noch als Subsystem. An seine Stelle ist Micrometer getreten.

## Versionen im Blick behalten

Fest verdrahtet sind WildFly 35.0.1.Final, Jakarta EE 10 und MicroProfile 6.1 -
alle als Properties im Wurzel-`pom.xml`. Bei einem WildFly-Upgrade lohnt der
Blick auf die Layer-Namen: sie verschieben sich zwischen den Versionen, und
Galleon meldet einen unbekannten Layer erst zur Bauzeit.

Die Datasource definiert jeder Service selbst per `@DataSourceDefinition` in
seiner Klasse `DataSourceConfig`, der JDBC-Treiber liegt als Abhaengigkeit im
WAR. Das kommt ohne zusaetzliches Feature-Pack und ohne CLI-Skript aus, hat
aber den Preis, dass Host, Benutzer und Passwort im Code stehen. Fuer Produktion
gehoert an diese Stelle eine serverseitig verwaltete Datasource; zu aendern
waere dann nur der JNDI-Name in den `persistence.xml`-Dateien.

Wichtig dabei: Die Klasse darf nicht im Default-Package liegen. Ohne
`package`-Deklaration wertet WildFly die Annotation kommentarlos nicht aus, und
die Datasource entsteht einfach nicht - ohne Fehlermeldung.
