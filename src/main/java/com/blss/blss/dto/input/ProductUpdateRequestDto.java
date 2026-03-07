package com.blss.blss.dto.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductUpdateRequestDto(
        @NotBlank
        String name,

        @NotNull(message = "Не указана цена")
        @Positive(message = "Цена должна быть положительной")
        BigDecimal price
) {
}
