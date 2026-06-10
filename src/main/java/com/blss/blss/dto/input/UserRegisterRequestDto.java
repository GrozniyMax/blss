package com.blss.blss.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterRequestDto(
        @NotBlank
        @Pattern(
                regexp = "[A-Za-z0-9]+",
                message = "username must contain only Latin letters and digits for Camunda login"
        )
        String username,

        @NotBlank
        @Size(min = 1)
        String password
) {
}
