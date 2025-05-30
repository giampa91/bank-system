package com.bank.system.payment_service;

import com.bank.system.payment_service.dto.PaymentCompletedEvent;
import com.bank.system.payment_service.kafka.PaymentProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentServiceApplicationTests {

	@Autowired
	private PaymentProducer producer;

	@Test
	void contextLoads() {
	}

	@Test
	void test() {
		PaymentCompletedEvent event = new PaymentCompletedEvent();
		event.setPaymentId("paymentId");
		producer.sendPaymentCompletedEvent(event);
	}

}
