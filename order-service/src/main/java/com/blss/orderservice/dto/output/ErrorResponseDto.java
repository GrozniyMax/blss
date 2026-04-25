package com.blss.orderservice.dto.output;

public record ErrorResponseDto(
        String message,
        String timestamp,
        String endpoint,
        String id
) {
}
