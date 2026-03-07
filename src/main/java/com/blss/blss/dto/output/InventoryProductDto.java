package com.blss.blss.dto.output;

public record InventoryProductDto(
        ProductDto product,
        Integer count
) {
}
