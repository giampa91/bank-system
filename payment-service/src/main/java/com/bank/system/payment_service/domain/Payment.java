package com.bank.system.payment_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint; // For composite unique constraints

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners; // To enable auditing listeners

@Entity // Marks this class as a JPA entity
@Table(name = "payment", uniqueConstraints = {
        // paymentId must be unique per tenant
        @UniqueConstraint(columnNames = {"tenant_id", "payment_id"}),
        // Idempotency key must be unique per tenant (prevents duplicate payments)
        @UniqueConstraint(columnNames = {"tenant_id", "idempotency_key"})
})
@EntityListeners(AuditingEntityListener.class) // Enables automatic @CreatedDate and @LastModifiedDate
public class Payment {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.UUID) // For UUID generation (Hibernate 6+ handles this)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false) // Crucial for multi-tenancy
    private String tenantId;

    @Column(name = "payment_id", nullable = false, length = 36) // Business-friendly ID, often a UUID string
    private String paymentId;

    @Column(name = "sender_account_id", nullable = false, length = 255)
    private String senderAccountId;

    @Column(name = "receiver_account_id", nullable = false, length = 255)
    private String receiverAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2) // Precision for monetary values
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3) // e.g., "USD", "EUR"
    private String currency;

    @Enumerated(EnumType.STRING) // Stores the enum as its String name in the DB
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 255) // Ensures unique payment requests
    private String idempotencyKey;

    @CreatedDate // Spring Data JPA annotation for automatic creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate // Spring Data JPA annotation for automatic update timestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Default constructor required by JPA
    public Payment() {
    }

    // Constructor for creating new payments (excluding generated fields and timestamps)
    public Payment(String tenantId, String paymentId, String senderAccountId, String receiverAccountId, BigDecimal amount, String currency, String idempotencyKey) {
        this.tenantId = tenantId;
        this.paymentId = paymentId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.INITIATED;
        this.idempotencyKey = idempotencyKey;
    }

    // Full constructor (useful for mapping from DB or tests)
    public Payment(UUID id, String tenantId, String paymentId, String senderAccountId, String receiverAccountId, BigDecimal amount, String currency, PaymentStatus status, String idempotencyKey, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
        // For JPA entities, equals and hashCode should primarily rely on the ID
        // once the entity is persisted. Before persistence, if ID is null,
        // you might use a business key (like paymentId + tenantId).
        // For simplicity and common JPA practice, we'll rely on the ID here.
        return id != null && Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
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