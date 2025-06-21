package com.bank.system.dtos.dto;

import java.util.UUID;

public class Event {
    private UUID eventId;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
}
