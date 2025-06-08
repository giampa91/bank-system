// src/main/java/com/bank/system/account_service/domain/Account.java
package com.bank.system.account_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint; // For composite unique constraints
import jakarta.persistence.EntityListeners; // To enable auditing listeners

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant; // Changed from LocalDateTime to Instant for JPA auditing best practice
import java.util.Objects;

@Entity // Marks this class as a JPA entity
@Table(name = "account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_number"})
})
@EntityListeners(AuditingEntityListener.class) // Enables automatic @CreatedDate and @LastModifiedDate
public class Account {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // For auto-incrementing Long IDs
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = false, length = 255) // Unique constraint is composite
    private String accountNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2) // Precision for monetary values
    private BigDecimal balance;

    @CreatedDate // Spring Data JPA annotation for automatic creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Changed to Instant

    @LastModifiedDate // Spring Data JPA annotation for automatic update timestamp
    @Column(name = "updated_at")
    private Instant updatedAt; // Changed to Instant

    // Default constructor required by JPA
    public Account() {
    }

    // Constructor for creating new accounts (excluding generated ID and auditing timestamps)
    public Account(String accountNumber, Long userId, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.balance = balance;
        // createdAt and updatedAt will be set automatically by @CreatedDate/@LastModifiedDate
    }

    // Full constructor (useful for mapping from DB or tests)
    public Account(Long id, String accountNumber, Long userId, BigDecimal balance, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() { // Changed to Instant
        return createdAt;
    }

    public Instant getUpdatedAt() { // Changed to Instant
        return updatedAt;
    }

    // Setters (for JPA to populate the object and for updates)
    public void setId(Long id) {
        this.id = id;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setCreatedAt(Instant createdAt) { // Changed to Instant
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) { // Changed to Instant
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        // For JPA entities, equals and hashCode should primarily rely on the ID
        // once the entity is persisted. Before persistence, if ID is null,
        // For simplicity and common JPA practice, we'll rely on the ID here.
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", userId=" + userId +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}