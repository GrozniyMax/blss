package com.blss.userservice.dto.output;

public record ErrorResponseDto(
        String error,
        String timestamp,
        String path,
        String status
) {
}
