package com.bank.system.dtos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SenderDebitedEvent extends Event {
    private UUID paymentId;
    private String accountId;
    private BigDecimal debitedAmount;
    private String currency;
    private Instant timestamp;

    public SenderDebitedEvent() {
    }

    public SenderDebitedEvent(UUID paymentId, String accountId, BigDecimal debitedAmount, String currency, Instant timestamp) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.debitedAmount = debitedAmount;
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

    public BigDecimal getDebitedAmount() {
        return debitedAmount;
    }

    public void setDebitedAmount(BigDecimal debitedAmount) {
        this.debitedAmount = debitedAmount;
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
        SenderDebitedEvent that = (SenderDebitedEvent) o;
        return Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(debitedAmount, that.debitedAmount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, accountId, debitedAmount, currency, timestamp);
    }

    @Override
    public String toString() {
        return "SenderDebitedEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", debitedAmount=" + debitedAmount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}