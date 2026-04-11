package com.example.rebooktradeservice.domain.outbox;

import com.example.rebooktradeservice.common.exception.TradeError;
import com.example.rebooktradeservice.common.exception.TradeException;
import com.example.rebooktradeservice.external.rabbitmq.message.NotificationTradeMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(NotificationTradeMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            Outbox outbox = Outbox.createNewOutbox(payload);
            outBoxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification message", e);
            throw new TradeException(TradeError.OUTBOX_SAVE_FAILED);
        }
    }
}
