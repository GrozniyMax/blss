package com.blss.blss.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record OrderCreateRequestDTO(
    UUID owner,
    UUID location,
    List<UUID> productIds
) { }
