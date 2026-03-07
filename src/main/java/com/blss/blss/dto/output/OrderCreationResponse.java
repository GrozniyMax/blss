package com.blss.blss.dto.output;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

public record OrderCreationResponse(
        UUID orderId
) { }
