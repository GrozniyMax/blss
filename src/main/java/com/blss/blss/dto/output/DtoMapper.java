package com.blss.blss.dto.output;

import com.blss.blss.domain.Product;
import com.blss.blss.service.OrderService;
import com.blss.blss.service.StoreService;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DtoMapper {

    GetOrderResponse toDto(OrderService.FullOrder order);

    ProductDto toDto(Product product);

    OrderPartDto toDto(OrderService.FullOrderItem orderPart);

    InventoryProductDto toDto(StoreService.InventoryProduct storeProduct);
}
