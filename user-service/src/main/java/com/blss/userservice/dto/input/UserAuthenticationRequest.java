package com.blss.userservice.dto.input;

public record UserAuthenticationRequest(
        String username,
        String password
) {
}
