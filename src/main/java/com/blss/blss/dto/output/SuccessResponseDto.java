package com.blss.blss.dto.output;

public record SuccessResponseDto(
        Object data,
        String timestamp,
        String endpoint
) {
}
