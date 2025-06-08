package com.bank.system.dtos.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PaymentFailedEvent {
    private UUID paymentId;
    private String reason;
    private Instant timestamp;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(UUID paymentId, String reason, Instant timestamp) {
        this.paymentId = paymentId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentFailedEvent that = (PaymentFailedEvent) o;
        return Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, reason, timestamp);
    }

    @Override
    public String toString() {
        return "PaymentFailedEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
