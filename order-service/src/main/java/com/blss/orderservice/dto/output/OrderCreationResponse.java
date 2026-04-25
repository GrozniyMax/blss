package com.blss.orderservice.dto.output;

import java.util.UUID;

public record OrderCreationResponse(
        UUID orderId
) { }
