package com.bank.system.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@SpringBootApplication
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

	/**
	 * Configures a JsonMessageConverter for Spring Kafka.
	 * This allows Spring Kafka to automatically convert JSON messages
	 * from Kafka topics into Java objects based on the payload.
	 * It's crucial when using JsonSerializer/JsonDeserializer.
	 *
	 * @return A RecordMessageConverter instance for JSON conversion.
	 */
	@Bean
	public RecordMessageConverter converter() {
		return new JsonMessageConverter();
	}

}
