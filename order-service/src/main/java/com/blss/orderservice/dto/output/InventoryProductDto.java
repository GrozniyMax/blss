package com.blss.orderservice.dto.output;

public record InventoryProductDto(
        ProductDto product,
        Integer count
) {
}
