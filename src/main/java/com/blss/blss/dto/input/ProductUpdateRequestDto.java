package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductUpdateRequestDto(
        @NotBlank
        String name,

        @NotNull
        BigDecimal price
) {
}
