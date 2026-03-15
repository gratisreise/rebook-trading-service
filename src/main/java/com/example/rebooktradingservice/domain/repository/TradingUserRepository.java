package com.example.rebooktradingservice.domain.repository;

import com.example.rebooktradingservice.domain.model.entity.Trading;
import com.example.rebooktradingservice.domain.model.entity.TradingUser;
import com.example.rebooktradingservice.domain.model.entity.compositekey.TradingUserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingUserRepository extends JpaRepository<TradingUser, TradingUserId> {

    @Query("select tu.trading from TradingUser tu where tu.tradingUserId.userId = :userId")
    Page<Trading> findTradingByUserId(String userId, Pageable pageable);

    boolean existsByTradingUserId(TradingUserId tradingUserId);
}
