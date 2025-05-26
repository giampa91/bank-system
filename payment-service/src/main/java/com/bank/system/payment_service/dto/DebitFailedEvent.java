package com.bank.system.payment_service.dto;

import java.time.Instant;
import java.util.Objects;

public class DebitFailedEvent {
    private String paymentId;
    private String accountId;
    private String reason;
    private Instant timestamp;

    public DebitFailedEvent() {
    }

    public DebitFailedEvent(String paymentId, String accountId, String reason, Instant timestamp) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
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
        DebitFailedEvent that = (DebitFailedEvent) o;
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
        return "DebitFailedEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
