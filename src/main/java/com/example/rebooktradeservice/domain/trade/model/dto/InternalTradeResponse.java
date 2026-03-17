package com.example.rebooktradeservice.domain.trade.model.dto;

import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;

public record InternalTradeResponse(
    Long tradeId,
    Long bookId,
    String userId,
    String title,
    Integer price,
    State state,
    String imageUrl
) {
    public static InternalTradeResponse from(Trade trade) {
        return new InternalTradeResponse(
            trade.getId(),
            trade.getBookId(),
            trade.getUserId(),
            trade.getTitle(),
            trade.getPrice(),
            trade.getState(),
            trade.getImageUrl()
        );
    }
}
