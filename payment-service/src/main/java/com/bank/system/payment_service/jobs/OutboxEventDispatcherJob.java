package com.bank.system.payment_service.jobs;

import com.bank.system.dtos.dto.CompensatePaymentEvent;
import com.bank.system.dtos.dto.PaymentCompletedEvent;
import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.dtos.dto.ReceiverCreditRequestEvent;
import com.bank.system.payment_service.domain.OutboxEvent;
import com.bank.system.payment_service.kafka.PaymentProducer;
import com.bank.system.payment_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxEventDispatcherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcherJob.class);

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final PaymentProducer paymentProducer;

    @Autowired
    public OutboxEventDispatcherJob(OutboxEventRepository outboxRepository,
                                    ObjectMapper objectMapper,
                                    PaymentProducer paymentProducer) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.paymentProducer = paymentProducer;
    }

    @Scheduled(fixedRate = 1000)
    public void dispatchEvents() {
        List<OutboxEvent> events = outboxRepository.fetchUnsentEvents(10);

        for (OutboxEvent event : events) {
            try {
                switch (event.getType()) {
                    case "PaymentInitiatedEvent" -> {
                        PaymentInitiatedEvent payload = objectMapper.readValue(event.getPayload(), PaymentInitiatedEvent.class);
                        paymentProducer.sendPaymentInitiatedEvent(payload)
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        boolean success = outboxRepository.markAsSent(event.getId(), event.getVersion());
                                        if (success) {
                                            log.info("Marked as sent: PaymentInitiatedEvent {}", event.getId());
                                        } else {
                                            log.warn("Version conflict: PaymentInitiatedEvent {} was already updated", event.getId());
                                        }
                                    } else {
                                        log.error("Failed to send PaymentInitiatedEvent {}", event.getId(), ex);
                                    }
                                });
                    }
                    case "PaymentCompletedEvent" -> {
                        PaymentCompletedEvent payload = objectMapper.readValue(event.getPayload(), PaymentCompletedEvent.class);
                        paymentProducer.sendPaymentCompletedEvent(payload)
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        boolean success = outboxRepository.markAsSent(event.getId(), event.getVersion());
                                        if (success) {
                                            log.info("Marked as sent: PaymentCompletedEvent {}", event.getId());
                                        } else {
                                            log.warn("Version conflict: PaymentCompletedEvent {} was already updated", event.getId());
                                        }
                                    } else {
                                        log.error("Failed to send PaymentCompletedEvent {}", event.getId(), ex);
                                    }
                                });
                    }
                    case "ReceiverCreditRequestEvent" -> {
                        ReceiverCreditRequestEvent payload = objectMapper.readValue(event.getPayload(), ReceiverCreditRequestEvent.class);
                        paymentProducer.sendReceiverCreditRequestEvent(payload)
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        boolean success = outboxRepository.markAsSent(event.getId(), event.getVersion());
                                        if (success) {
                                            log.info("Marked as sent: ReceiverCreditRequestEvent {}", event.getId());
                                        } else {
                                            log.warn("Version conflict: ReceiverCreditRequestEvent {} was already updated", event.getId());
                                        }
                                    } else {
                                        log.error("Failed to send ReceiverCreditRequestEvent {}", event.getId(), ex);
                                    }
                                });
                    }
                    case "CompensatePaymentEvent" -> {
                        CompensatePaymentEvent payload = objectMapper.readValue(event.getPayload(), CompensatePaymentEvent.class);
                        paymentProducer.sendCompensatePaymentEvent(payload)
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        boolean success = outboxRepository.markAsSent(event.getId(), event.getVersion());
                                        if (success) {
                                            log.info("Marked as sent: CompensatePaymentEvent {}", event.getId());
                                        } else {
                                            log.warn("Version conflict: CompensatePaymentEvent {} was already updated", event.getId());
                                        }
                                    } else {
                                        log.error("Failed to send CompensatePaymentEvent {}", event.getId(), ex);
                                    }
                                });
                    }
                    default -> {
                        log.warn("Unknown outbox event type {} for event {}", event.getType(), event.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing outbox event {}", event.getId(), e);
            }
        }
    }
}
