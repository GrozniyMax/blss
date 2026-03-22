package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemDeliveredDto(

        @NotNull(message = "Не указан id элемента заказа")
        UUID itemId,

        @NotBlank(message = "Не указана ячейка")
        String yacheyka
) {  }
