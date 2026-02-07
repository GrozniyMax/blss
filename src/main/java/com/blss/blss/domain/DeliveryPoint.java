package com.blss.blss.domain;

import org.springframework.data.annotation.Id;

import java.util.UUID;

public record DeliveryPoint(
        @Id
        UUID id,

        String name
) { }
