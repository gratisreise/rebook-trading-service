package com.example.rebooktradeservice.domain.trade.service.writer;

import com.example.rebooktradeservice.common.enums.BookCondition;
import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.common.exception.TradeError;
import com.example.rebooktradeservice.common.exception.TradeException;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;
import com.example.rebooktradeservice.domain.trade.repository.TradeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeWriter {

    private final TradeRepository tradeRepository;

    @Transactional
    public Trade save(Trade trade) {
        return tradeRepository.save(trade);
    }

    @Transactional
    public List<Trade> saveAll(List<Trade> trades) {
        return tradeRepository.saveAll(trades);
    }

    @Transactional
    public void deleteById(Long tradeId) {
        tradeRepository.deleteById(tradeId);
    }

    @Transactional
    public void updateConditionAndState(Long tradeId, BookCondition condition, State state) {
        Trade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new TradeException(TradeError.TRADE_NOT_FOUND));
        trade.setRating(condition.name());
        trade.setState(state);
    }

    @Transactional
    public void updateTitleAndContent(Long tradeId, String title, String content) {
        Trade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new TradeException(TradeError.TRADE_NOT_FOUND));
        trade.setTitle(title);
        trade.setContent(content);
    }
}
