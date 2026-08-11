package com.atwms.common.outbox;

public enum OutboxStatus {
    NEW,     // geschrieben, noch nicht übertragen
    SENT,    // von Kafka bestätigt
    FAILED   // nach mehreren Versuchen endgültig gescheitert
}