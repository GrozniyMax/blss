package com.blss.orderservice.client.dto;

import com.blss.orderservice.security.Role;

import java.util.List;

public record UserValidationResponse(
        String username,
        boolean exists,
        boolean enabled,
        List<Role> roles
) {
}
