package com.atwms.common.api;

import java.time.Instant;

/**
 * Einheitliches Fehlerformat aller Services (RFC-7807-nah, aber bewusst schlank).
 */
public class ApiError {

    private String code;
    private String message;
    private Instant timestamp = Instant.now();

    public ApiError() {
    }

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
