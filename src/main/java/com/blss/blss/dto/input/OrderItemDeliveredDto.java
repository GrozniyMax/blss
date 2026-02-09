package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemDeliveredDto(

        @NotNull
        UUID itemId,

        @NotNull
        @NotBlank
        String yacheyka
) {  }
