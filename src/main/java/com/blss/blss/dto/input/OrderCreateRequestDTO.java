package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequestDTO(
        @NotNull(message = "Не указан Id заказа")
        UUID owner,

        @NotNull(message = "Не указан Id ПВЗ для доставки")
        UUID location,

        @NotNull(message = "Не указаны товары")
        @Size(min = 1, message = "Не указаны товары")
        List<UUID> productIds) {
}
