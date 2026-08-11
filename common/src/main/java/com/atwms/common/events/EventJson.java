package com.atwms.common.events;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * Serialisierung der Integrationsevents als JSON-String.
 *
 * <p>Bewusst String statt eines Kafka-spezifischen Serializers: dadurch bleibt
 * die Anwendung unabhaengig davon, welcher Serializer in der jeweiligen
 * Runtime verfuegbar ist, und die Nachrichten sind in Kafka-Tools direkt
 * lesbar. Fuer groessere Volumina waere Avro oder Protobuf mit Schema-Registry
 * der naechste Schritt.</p>
 */
public final class EventJson {

    private static final Jsonb JSONB = JsonbBuilder.create(
            new JsonbConfig().withFormatting(false));

    private EventJson() {
    }

    public static String toJson(Object event) {
        return JSONB.toJson(event);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return JSONB.fromJson(json, type);
    }
}
