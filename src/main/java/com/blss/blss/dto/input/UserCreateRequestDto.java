package com.blss.blss.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequestDto(
        @NotBlank
        @Email
        String email
) {
}
