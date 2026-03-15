package com.example.rebooktradingservice.domain.service;

import com.example.rebooktradingservice.exception.CMissingDataException;
import com.example.rebooktradingservice.domain.model.dto.TradingResponse;
import com.example.rebooktradingservice.domain.model.entity.Trading;
import com.example.rebooktradingservice.domain.model.entity.TradingUser;
import com.example.rebooktradingservice.domain.model.entity.compositekey.TradingUserId;
import com.example.rebooktradingservice.domain.repository.TradingRepository;
import com.example.rebooktradingservice.domain.repository.TradingUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradingUserService {

    private final TradingUserRepository tradingUserRepository;
    private final TradingRepository tradingRepository;


    @Transactional
    public void tradingMark(String userId, Long tradingId) {
        TradingUserId tradingUserId = new TradingUserId(tradingId, userId);
        if (!tradingRepository.existsById(tradingId)) {
            tradingUserRepository.deleteById(tradingUserId);
            return;
        }
        if (tradingUserRepository.existsById(tradingUserId)) {
            tradingUserRepository.deleteById(tradingUserId);
        } else {
            Trading trading = tradingRepository.findById(tradingId)
                .orElseThrow(CMissingDataException::new);
            TradingUser tradingUser = new TradingUser(tradingUserId, trading);
            tradingUserRepository.save(tradingUser);
        }
    }

    public Page<TradingResponse> getMarkedTradings(String userId, Pageable pageable) {
        Page<Trading> markedTradings = tradingUserRepository.findTradingByUserId(userId, pageable);
        return markedTradings.map(TradingResponse::new);
    }
}
