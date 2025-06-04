package com.bank.system.payment_service.domain;

public enum PaymentStatus {
    INITIATED,
    SENDER_DEBITED,
    RECEIVER_CREDITED,
    COMPLETED,
    FAILED,
    CREDIT_FAILED,
    DEBIT_FAILED,
    CANCELLED
}