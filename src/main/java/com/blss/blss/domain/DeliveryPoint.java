package com.blss.blss.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("delivery_point")
public record DeliveryPoint(
        @Id
        UUID id,

        String name,

        String address
) { }
