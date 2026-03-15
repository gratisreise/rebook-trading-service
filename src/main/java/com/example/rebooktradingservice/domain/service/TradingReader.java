package com.example.rebooktradingservice.domain.service;

import com.example.rebooktradingservice.common.PageResponse;
import com.example.rebooktradingservice.exception.CMissingDataException;
import com.example.rebooktradingservice.domain.model.dto.TradingResponse;
import com.example.rebooktradingservice.domain.model.entity.Trading;
import com.example.rebooktradingservice.domain.repository.TradingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TradingReader {
    private final TradingRepository tradingRepository;

    public Trading findById(Long tradingId) {
        return tradingRepository.findById(tradingId)
            .orElseThrow(CMissingDataException::new);
    }

    public Page<Trading> readTradings(String userId, Pageable pageable) {
        return tradingRepository.findByUserId(userId, pageable);
    }

    public Page<Trading> getAllTradings(Long bookId, Pageable pageable) {
        return tradingRepository.findByBookId(bookId, pageable);
    }

    public Page<Trading> getRecommendations(List<Long> bookIds, Pageable pageable) {
        return tradingRepository.findByBookIdIn(bookIds, pageable);
    }

    public PageResponse<TradingResponse> getOthersTradings(String userId, Pageable pageable) {
        Page<Trading> tradings = tradingRepository.findByUserId(userId, pageable);
        Page<TradingResponse> responses = tradings.map(TradingResponse::new);
        return new PageResponse<>(responses);
    }
}
