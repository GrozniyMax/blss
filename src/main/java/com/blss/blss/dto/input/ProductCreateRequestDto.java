package com.blss.blss.dto.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductCreateRequestDto(

        @NotBlank(message = "Непустое имя товара обязательно")
        String name,

        @NotNull(message = "Цена обязательна")
        @Positive(message = "Цена должна быть не отрицательной")
        BigDecimal price,

        @NotNull
        @Positive(message = "Количество не дожно быть отрицательным")
        @Max(value = 10_000_000, message = "Кол-во не более 10млн")
        Integer initialCount
) {
}
