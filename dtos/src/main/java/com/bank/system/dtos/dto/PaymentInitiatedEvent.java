package com.bank.system.dtos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class PaymentInitiatedEvent {
    private String paymentId;
    private String senderAccountId;
    private String receiverAccountId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private Instant timestamp;

    public PaymentInitiatedEvent() {
    }

    public PaymentInitiatedEvent(String paymentId, String senderAccountId, String receiverAccountId, BigDecimal amount, String currency, String idempotencyKey, Instant timestamp) {
        this.paymentId = paymentId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.timestamp = timestamp;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(String senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getReceiverAccountId() {
        return receiverAccountId;
    }

    public void setReceiverAccountId(String receiverAccountId) {
        this.receiverAccountId = receiverAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
        PaymentInitiatedEvent that = (PaymentInitiatedEvent) o;
        return Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(senderAccountId, that.senderAccountId) &&
                Objects.equals(receiverAccountId, that.receiverAccountId) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(idempotencyKey, that.idempotencyKey) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, senderAccountId, receiverAccountId, amount, currency, idempotencyKey, timestamp);
    }

    @Override
    public String toString() {
        return "PaymentInitiatedEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", senderAccountId='" + senderAccountId + '\'' +
                ", receiverAccountId='" + receiverAccountId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}