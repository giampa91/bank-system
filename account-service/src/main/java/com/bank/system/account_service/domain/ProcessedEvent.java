package com.bank.system.account_service.domain;

import java.time.Instant;
import java.util.UUID;

public class ProcessedEvent {
    private UUID eventId;
    private String eventType;
    private String payload;
    private Instant processedAt;

    public ProcessedEvent() {}

    public ProcessedEvent(UUID eventId, String eventType, String payload, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.processedAt = processedAt;
    }

    // getters & setters
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
