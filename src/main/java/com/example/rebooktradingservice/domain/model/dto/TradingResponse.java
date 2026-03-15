package com.example.rebooktradingservice.domain.model.dto;

import com.example.rebooktradingservice.common.enums.State;
import com.example.rebooktradingservice.domain.model.entity.Trading;
import java.time.LocalDateTime;

public record TradingResponse(
    Long tradingId,
    Long bookId,
    String userId,
    String title,
    String content,
    String rating,
    Integer price,
    State state,
    String imageUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isMarked
) {
    public TradingResponse(Trading trading) {
        this(
            trading.getId(),
            trading.getBookId(),
            trading.getUserId(),
            trading.getTitle(),
            trading.getContent(),
            trading.getRating(),
            trading.getPrice(),
            trading.getState(),
            trading.getImageUrl(),
            trading.getCreatedAt(),
            trading.getUpdatedAt(),
            false
        );
    }

    public TradingResponse withMarked(boolean marked) {
        return new TradingResponse(
            tradingId,
            bookId,
            userId,
            title,
            content,
            rating,
            price,
            state,
            imageUrl,
            createdAt,
            updatedAt,
            marked
        );
    }
}
