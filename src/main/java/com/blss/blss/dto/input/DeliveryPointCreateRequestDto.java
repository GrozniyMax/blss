package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryPointCreateRequestDto(
        @NotBlank(message = "Поле name обязательное")
        @Size(max = 30)
        String name,

        @NotBlank(message = "Поле address обязательное")
        @Size(max = 30)
        String address
) {
}
