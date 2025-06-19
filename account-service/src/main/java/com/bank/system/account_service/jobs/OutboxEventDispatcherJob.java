package com.bank.system.account_service.jobs;

import com.bank.system.account_service.domain.OutboxEvent;
import com.bank.system.account_service.kafka.AccountProducer;
import com.bank.system.account_service.repository.OutboxEventRepository;
import com.bank.system.dtos.dto.*; // Ensure all DTOs are imported
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture; // Import CompletableFuture

import static com.bank.system.account_service.service.AccountTransactionalService.*; // Import all static event types

@Component
public class OutboxEventDispatcherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcherJob.class);

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final AccountProducer accountProducer;

    @Autowired
    public OutboxEventDispatcherJob(OutboxEventRepository outboxRepository,
                                    ObjectMapper objectMapper,
                                    AccountProducer accountProducer) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.accountProducer = accountProducer;
    }

    @Scheduled(fixedRate = 5000)
    public void dispatchEvents() {
        List<OutboxEvent> events = outboxRepository.fetchUnsentEvents(10);
        for (OutboxEvent event : events) {
            try {
                // Common completion logic to avoid repetition
                CompletableFuture<?> future;
                String eventType = event.getType();

                switch (eventType) {
                    case SENDER_DEBITED_FAILED_EVENT -> {
                        DebitFailedEvent payload = objectMapper.readValue(event.getPayload(), DebitFailedEvent.class);
                        future = accountProducer.sendSenderDebitedFailedEvent(payload);
                    }
                    case SENDER_DEBITED_EVENT -> {
                        SenderDebitedEvent payload = objectMapper.readValue(event.getPayload(), SenderDebitedEvent.class);
                        future = accountProducer.sendSenderDebitedEvent(payload);
                    }
                    case RECEIVER_CREDIT_EVENT -> {
                        ReceiverCreditEvent payload = objectMapper.readValue(event.getPayload(), ReceiverCreditEvent.class);
                        future = accountProducer.sendReceiverCreditEvent(payload);
                    }
                    case COMPENSATE_PAYMENT_EVENT -> {
                        CompensatePaymentEvent payload = objectMapper.readValue(event.getPayload(), CompensatePaymentEvent.class);
                        future = accountProducer.sendCompensatePaymentEvent(payload);
                    }
                    default -> {
                        log.warn("Unknown outbox event type {} for event {}", event.getType(), event.getId());
                        continue; // Skip to the next event
                    }
                }

                // Handle the completion of the Kafka send operation
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        boolean success = outboxRepository.markAsSent(event.getId(), event.getVersion());
                        if (success) {
                            log.info("Marked as sent: {} for event ID {}", eventType, event.getId());
                        } else {
                            log.warn("Version conflict: {} with event ID {} was already updated", eventType, event.getId());
                        }
                    } else {
                        log.error("Failed to send {} for event ID {}", eventType, event.getId(), ex);
                    }
                });

            } catch (Exception e) {
                log.error("Error processing outbox event {} of type {}: {}", event.getId(), event.getType(), e.getMessage(), e);
            }
        }
    }
}