package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;

public record DeliveryPointCreateRequestDto(
        @NotBlank
        String name,

        @NotBlank
        String address
) {
}
