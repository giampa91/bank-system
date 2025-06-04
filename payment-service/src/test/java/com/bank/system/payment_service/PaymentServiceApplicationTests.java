package com.bank.system.payment_service;

import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.payment_service.kafka.PaymentProducer;
import com.bank.system.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class PaymentServiceApplicationTests {

	@Autowired
	private PaymentProducer producer;

	@Autowired
	private PaymentService paymentService;

	@Test
	void contextLoads() {
	}

//	@Test
//	void test() {
//
//		PaymentInitiatedEvent event = new PaymentInitiatedEvent();
//		event.setPaymentId("paymentId2");
//		event.setAmount(BigDecimal.valueOf(10));
//		event.setCurrency("eur");
//		event.setIdempotencyKey("idempotencyKey1");
//		event.setSenderAccountId("ACC-001-A");
//		event.setReceiverAccountId("ACC-002-B");
//		producer.sendPaymentInitiatedEvent(event);
//
//	}

}
