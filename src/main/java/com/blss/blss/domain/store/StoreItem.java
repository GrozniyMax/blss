package com.blss.blss.domain.store;

import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("store")
public record StoreItem(
        UUID productId,
        Integer count
) {
}
