package com.blss.userservice.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequestDto(
        @NotBlank
        String username,

        @NotBlank
        @Size(min = 1)
        String password
) {
}
