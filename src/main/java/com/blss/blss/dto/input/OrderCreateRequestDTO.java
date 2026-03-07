package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequestDTO(
        @NotNull
        UUID owner,

        @NotNull
        UUID location,

        @NotNull
        @Size(min = 1)
        List<UUID> productIds) {
}
