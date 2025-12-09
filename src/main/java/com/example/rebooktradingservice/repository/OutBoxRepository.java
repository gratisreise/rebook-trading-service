package com.example.rebooktradingservice.repository;


import com.example.rebooktradingservice.enums.MessageStatus;
import com.example.rebooktradingservice.model.entity.Outbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutBoxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findTop20ByStatusOrderByCreatedAtAsc(MessageStatus messageStatus);
}
