package com.blss.blss.dto.output;

public record ErrorResponseDto(
        String message,
        String timestamp,
        String endpoint,
        String id
) {
}
