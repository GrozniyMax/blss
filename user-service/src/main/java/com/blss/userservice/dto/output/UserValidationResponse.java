package com.blss.userservice.dto.output;

import com.blss.userservice.security.Role;

import java.util.List;

public record UserValidationResponse(
        String username,
        boolean exists,
        boolean enabled,
        List<Role> roles
) {
}
