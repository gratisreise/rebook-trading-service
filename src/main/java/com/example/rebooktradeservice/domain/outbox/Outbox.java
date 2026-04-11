package com.example.rebooktradeservice.domain.outbox;

import com.example.rebooktradeservice.common.enums.MessageStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String payload;

    @Enumerated(value = EnumType.STRING)
    private MessageStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (status == null) {
            this.status = MessageStatus.PENDING;
        }
    }

    public static Outbox createNewOutbox(String payload){
        return Outbox.builder()
            .payload(payload)
            .status(MessageStatus.PENDING)
            .build();
    }
}
