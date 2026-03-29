package com.blss.blss.dto.input;

import com.blss.blss.security.Role;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserUpdateRequestDto(
        @Size(min = 1)
        String password,

        List<Role> roles,

        Boolean enabled
) {
}
