package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductCreateRequestDto(
        @NotBlank
        String name,

        @NotNull
        BigDecimal price,

        @NotNull
        @Positive
        Integer initialCount
) {
}
