package com.example.rebooktradingservice.domain.model.dto;

import com.example.rebooktradingservice.common.enums.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record TradingRequest(
    @NotNull
    Long bookId,

    @NotBlank
    @Length(min = 3, max = 100)
    String title,

    @NotBlank
    @Length(min = 3, max = 800)
    String content,

    @NotBlank
    @Length(min = 1, max = 5)
    String rating,

    int price,

    @NotNull
    State state
) {
}
