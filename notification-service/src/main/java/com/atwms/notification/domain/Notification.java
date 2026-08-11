package com.atwms.notification.domain;

import java.time.Instant;

public class Notification {

    private String tenantId;
    private String subject;
    private String body;
    private Instant sentAt = Instant.now();

    public Notification() {
    }

    public Notification(String tenantId, String subject, String body) {
        this.tenantId = tenantId;
        this.subject = subject;
        this.body = body;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
