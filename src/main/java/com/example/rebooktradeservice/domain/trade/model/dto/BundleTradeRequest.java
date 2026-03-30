package com.example.rebooktradeservice.domain.trade.model.dto;

import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BundleTradeRequest(
    @NotEmpty @Valid
    List<TradeItem> trades
) {
    public record TradeItem(
        @NotNull Long bookId,
        @NotBlank String bookTitle,
        @NotBlank String author,
        @NotBlank String isbn,
        @NotNull Integer price,
        @NotBlank String imageUrl
    ) {
        public Trade toEntity(String userId) {
            return Trade.builder()
                .bookId(bookId)
                .userId(userId)
                .title("AI 평가 대기 중")
                .content("AI 평가 대기 중")
                .imageUrl(imageUrl)
                .rating("PENDING")
                .price(price)
                .state(State.WAITING)
                .build();
        }
    }
}
