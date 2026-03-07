package com.blss.blss.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.blss.blss.validation.StrictEmail;
import jakarta.validation.constraints.Pattern;

public record UserCreateRequestDto(
        @NotBlank
        @Pattern(regexp="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        String email
) {
}
