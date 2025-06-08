package com.bank.system.account_service.kafka;

import com.bank.system.dtos.dto.SenderDebitedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class AccountProducerTest {

    @Autowired
    AccountProducer accountProducer;

}