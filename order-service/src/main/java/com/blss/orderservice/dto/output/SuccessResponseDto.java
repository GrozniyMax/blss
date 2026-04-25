package com.blss.orderservice.dto.output;

public record SuccessResponseDto(
        Object data,
        String timestamp,
        String endpoint
) {
}
