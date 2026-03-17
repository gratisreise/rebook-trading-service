package com.example.rebooktradeservice.domain.trade.controller;

import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.domain.trade.model.dto.InternalTradeResponse;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;
import com.example.rebooktradeservice.domain.trade.repository.TradeRepository;
import com.example.rebooktradeservice.domain.trade.service.reader.TradeReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/trades")
public class InternalTradeController {

    private final TradeReader tradeReader;
    private final TradeRepository tradeRepository;

    @GetMapping("/{tradeId}")
    public InternalTradeResponse getTrade(@PathVariable Long tradeId) {
        Trade trade = tradeReader.findById(tradeId);
        return InternalTradeResponse.from(trade);
    }

    @GetMapping("/book/{bookId}/count")
    public Integer getActiveTradeCountByBook(@PathVariable Long bookId) {
        return tradeRepository.countByBookIdAndState(bookId, State.AVAILABLE);
    }

    @GetMapping("/user/{userId}/count")
    public Integer getTradeCountByUser(@PathVariable String userId) {
        return tradeRepository.countByUserId(userId);
    }

    @GetMapping("/user/{userId}/list")
    public List<InternalTradeResponse> getTradesByUser(@PathVariable String userId) {
        return tradeRepository.findByUserId(userId).stream()
            .map(InternalTradeResponse::from)
            .toList();
    }
}
