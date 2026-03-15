package com.example.rebooktradingservice.domain.repository;

import com.example.rebooktradingservice.domain.model.entity.Trading;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingRepository extends JpaRepository<Trading, Long> {

    Page<Trading> findByUserId(String userId, Pageable pageable);

    Page<Trading> findByBookId(Long bookId, Pageable pageable);

    Page<Trading> findByBookIdIn(List<Long> bookIds, Pageable pageable);
}
