package com.example.rebooktradingservice.domain.model.entity.compositekey;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradingUserId implements Serializable {

    private Long tradingId;
    private String userId;
}
