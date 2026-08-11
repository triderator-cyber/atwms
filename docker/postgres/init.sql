-- Database per Service: jeder Microservice bekommt seine eigene Datenbank.
-- Kein Service greift auf die Tabellen eines anderen zu - der einzige Weg
-- fuehrt ueber dessen REST-API oder ueber ein Event.

CREATE DATABASE tenantdb OWNER atwms;
CREATE DATABASE orderdb OWNER atwms;
CREATE DATABASE billingdb OWNER atwms;

-- ---------------------------------------------------------------------------
-- Optionale dritte Sicherheitsebene: Row Level Security in PostgreSQL.
--
-- Die Anwendung filtert bereits ueber die Spalte tenant_id (siehe
-- TenantAwareEntity und die NamedQueries). RLS zieht zusaetzlich in der
-- Datenbank eine Grenze: selbst eine vergessene WHERE-Klausel oder ein
-- direkter psql-Zugriff koennen dann keine fremden Mandantendaten liefern.
--
-- Die Anwendung muss dafuer pro Transaktion die Sitzungsvariable setzen:
--     SET LOCAL app.tenant_id = '<mandant>';
--
-- Bewusst auskommentiert: erst aktivieren, wenn das Setzen der Variable in
-- einem JTA-Interceptor implementiert ist - sonst liefert jede Query
-- ploetzlich null Zeilen.
--
-- \connect orderdb
-- ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY tenant_isolation ON orders
--     USING (tenant_id = current_setting('app.tenant_id', true));
-- ---------------------------------------------------------------------------
