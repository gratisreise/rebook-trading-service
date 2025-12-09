package com.example.rebooktradingservice.utils;



import com.example.rebooktradingservice.enums.MessageStatus;
import com.example.rebooktradingservice.model.entity.Outbox;
import com.example.rebooktradingservice.model.message.NotificationTradeMessage;
import com.example.rebooktradingservice.repository.OutBoxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final OutBoxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${notification.exchange}")
    private String exchange;

    @Value("${notification.routing-key}")
    private String routingKey;

    @Transactional
    @Scheduled(fixedDelay = 2000) // 2초마다 실행
    public void processOutbox() {
        log.info("스케줄 실행!!");

        List<Outbox> pendingEvents =
            outboxRepository.findTop20ByStatusOrderByCreatedAtAsc(MessageStatus.PENDING);

        for (Outbox event : pendingEvents) {
            try {
                NotificationTradeMessage message = objectMapper.readValue(event.getPayload(), NotificationTradeMessage.class);
                rabbitTemplate.convertAndSend(exchange, routingKey, message);

                event.setStatus(MessageStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);

            } catch (Exception e) {
                // 실패 시 상태만 FAILED 로 변경 (DLQ 와 함께 사용 가능)
                event.setStatus(MessageStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }
}