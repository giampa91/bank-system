package com.bank.system.dtos.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CreditFailedEvent {
    private UUID paymentId;
    private String accountId;
    private String reason;
    private Instant timestamp;

    public CreditFailedEvent() {
    }

    public CreditFailedEvent(UUID paymentId, String accountId, String reason, Instant timestamp) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
        CreditFailedEvent that = (CreditFailedEvent) o;
        return Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, accountId, reason, timestamp);
    }

    @Override
    public String toString() {
        return "CreditFailedEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}