package com.example.rebooktradeservice.domain.trade.service.reader;

import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.common.exception.TradeError;
import com.example.rebooktradeservice.common.exception.TradeException;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;
import com.example.rebooktradeservice.domain.trade.repository.TradeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeReader {

    private final TradeRepository tradeRepository;

    public Trade findById(Long tradeId) {
        return tradeRepository.findById(tradeId)
            .orElseThrow(() -> new TradeException(TradeError.TRADE_NOT_FOUND));
    }

    public boolean existsById(Long tradeId) {
        return tradeRepository.existsById(tradeId);
    }

    public Page<Trade> findByUserId(String userId, Pageable pageable) {
        return tradeRepository.findByUserId(userId, pageable);
    }

    public Page<Trade> findByBookId(Long bookId, Pageable pageable) {
        return tradeRepository.findByBookId(bookId, pageable);
    }

    public Page<Trade> findByBookIdIn(List<Long> bookIds, Pageable pageable) {
        return tradeRepository.findByBookIdIn(bookIds, pageable);
    }

    public Page<Trade> findByUserIdAndState(String userId, State state, Pageable pageable) {
        return tradeRepository.findByUserIdAndState(userId, state, pageable);
    }

    public List<Trade> findByUserIdAndState(String userId, State state) {
        return tradeRepository.findByUserIdAndState(userId, state);
    }
}
