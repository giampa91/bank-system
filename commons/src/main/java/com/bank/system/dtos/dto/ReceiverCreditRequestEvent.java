package com.bank.system.dtos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ReceiverCreditRequestEvent extends Event {
    private UUID paymentId;
    private String accountId;
    private BigDecimal creditedAmount;
    private String currency;
    private Instant timestamp;

    public ReceiverCreditRequestEvent() {
    }

    public ReceiverCreditRequestEvent(UUID paymentId, String accountId, BigDecimal creditedAmount, String currency, Instant timestamp) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.creditedAmount = creditedAmount;
        this.currency = currency;
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

    public BigDecimal getCreditedAmount() {
        return creditedAmount;
    }

    public void setCreditedAmount(BigDecimal creditedAmount) {
        this.creditedAmount = creditedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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
        ReceiverCreditRequestEvent that = (ReceiverCreditRequestEvent) o;
        return Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(creditedAmount, that.creditedAmount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, accountId, creditedAmount, currency, timestamp);
    }

    @Override
    public String toString() {
        return "ReceiverCreditedEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", creditedAmount=" + creditedAmount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}