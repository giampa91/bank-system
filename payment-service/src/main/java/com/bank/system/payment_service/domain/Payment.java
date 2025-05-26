package com.bank.system.payment_service.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Payment {
    private UUID id; // Internal UUID for database
    private String paymentId; // Business-friendly ID, often UUID as string
    private String senderAccountId;
    private String receiverAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;

    public Payment() {
    }

    public Payment(UUID id, String paymentId, String senderAccountId, String receiverAccountId, BigDecimal amount, String currency, PaymentStatus status, String idempotencyKey, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id) &&
                Objects.equals(paymentId, payment.paymentId) &&
                Objects.equals(senderAccountId, payment.senderAccountId) &&
                Objects.equals(receiverAccountId, payment.receiverAccountId) &&
                Objects.equals(amount, payment.amount) &&
                Objects.equals(currency, payment.currency) &&
                status == payment.status &&
                Objects.equals(idempotencyKey, payment.idempotencyKey) &&
                Objects.equals(createdAt, payment.createdAt) &&
                Objects.equals(updatedAt, payment.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, paymentId, senderAccountId, receiverAccountId, amount, currency, status, idempotencyKey, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", paymentId='" + paymentId + '\'' +
                ", senderAccountId='" + senderAccountId + '\'' +
                ", receiverAccountId='" + receiverAccountId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
