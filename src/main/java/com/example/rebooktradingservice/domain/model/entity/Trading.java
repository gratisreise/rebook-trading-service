package com.example.rebooktradingservice.domain.model.entity;

import com.example.rebooktradingservice.common.enums.State;
import com.example.rebooktradingservice.domain.model.dto.TradingRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@Builder
public class Trading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 800)
    private String content;

    @Column(nullable = false, length = 400)
    private String imageUrl;

    @Column(nullable = false, length = 5)
    private String rating;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Trading(TradingRequest request, String imageUrl, String userId) {
        this.bookId = request.bookId();
        this.userId = userId;
        this.title = request.title();
        this.content = request.content();
        this.imageUrl = imageUrl;
        this.rating = request.rating();
        this.price = request.price();
        this.state = request.state();
    }

    public void update(TradingRequest request, String userId) {
        this.bookId = request.bookId();
        this.userId = userId;
        this.title = request.title();
        this.content = request.content();
        this.rating = request.rating();
        this.price = request.price();
        this.state = request.state();
    }
}
