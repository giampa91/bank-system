package com.bank.system.account_service.service;

import com.bank.system.account_service.domain.OutboxEvent;
import com.bank.system.account_service.repository.AccountRepository;
import com.bank.system.account_service.repository.OutboxEventRepository;
import com.bank.system.dtos.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.producer.internals.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;


@Service
public class AccountTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAccountService.class);
    public static final String SENDER_DEBITED_FAILED_EVENT = "SenderDebitedFailedEvent";
    public static final String SENDER_DEBITED_EVENT = "SenderDebitedEvent";
    public static final String RECEIVER_CREDIT_EVENT = "ReceiverCreditEvent";
    public static final String COMPENSATE_PAYMENT_EVENT = "CompensatePaymentEvent";
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AccountTransactionalService(OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void sendCompensatePaymentEvent(CompensatePaymentEvent event) {
        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize " + COMPENSATE_PAYMENT_EVENT, e);
        }
        UUID id = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        outboxEventRepository.save(new OutboxEvent(
                id,
                "Payment",
                entityId, // handle entityId
                COMPENSATE_PAYMENT_EVENT,
                eventPayload,
                Instant.now(),
                false
        ));
        log.debug("Outbox event saved for Entity id: {}", entityId);
    }

    @Transactional
    public void sendReceiverCreditEvent(ReceiverCreditEvent event) {
        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize " + RECEIVER_CREDIT_EVENT, e);
        }
        UUID id = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        outboxEventRepository.save(new OutboxEvent(
                id,
                "Payment",
                entityId, // handle entityId
                RECEIVER_CREDIT_EVENT,
                eventPayload,
                Instant.now(),
                false
        ));
        log.debug("Outbox event saved for Entity id: {}", entityId);
    }

    @Transactional
    public void sendSenderDebitedEvent(SenderDebitedEvent event) {
        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize " + SENDER_DEBITED_EVENT, e);
        }
        UUID id = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        outboxEventRepository.save(new OutboxEvent(
                id,
                "Payment",
                entityId, // handle entityId
                SENDER_DEBITED_EVENT,
                eventPayload,
                Instant.now(),
                false
        ));
        log.debug("Outbox event saved for Entity id: {}", entityId);
    }

    @Transactional
    public void sendSenderDebitedFailedEvent(DebitFailedEvent event) {
        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize " + SENDER_DEBITED_FAILED_EVENT, e);
        }
        UUID id = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        outboxEventRepository.save(new OutboxEvent(
                id,
                "Payment",
                entityId, // handle entityId
                SENDER_DEBITED_FAILED_EVENT,
                eventPayload,
                Instant.now(),
                false
        ));
        log.debug("Outbox event saved for Entity id: {}", entityId);
    }
}

