package com.example.rebooktradingservice.domain.model.message;

public record NotificationTradeMessage(
    String message,
    String type,
    String tradingId,
    String bookId
) {
    public NotificationTradeMessage(Long tradingId, String content, Long bookId) {
        this(content, "TRADE", tradingId.toString(), bookId.toString());
    }
}
