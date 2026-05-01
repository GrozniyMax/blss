package com.blss.statusservice.client.dto;


import com.blss.statusservice.security.Role;

import java.util.List;

public record UserValidationResponse(
        String username,
        boolean exists,
        boolean enabled,
        List<Role> roles
) {
}
