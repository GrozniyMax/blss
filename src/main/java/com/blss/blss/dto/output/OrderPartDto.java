package com.blss.blss.dto.output;

import java.util.UUID;

public record OrderPartDto(
        UUID id,
        String yacheyka,
        ProductDto product
) { }
