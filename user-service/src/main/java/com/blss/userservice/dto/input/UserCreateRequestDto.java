package com.blss.userservice.dto.input;

import com.blss.userservice.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequestDto(
        @NotBlank
        String username,

        @NotBlank
        @Size(min = 1)
        String password,

        List<Role> roles
) {
}
