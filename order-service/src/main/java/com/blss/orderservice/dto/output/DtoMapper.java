package com.blss.orderservice.dto.output;

import com.blss.orderservice.domain.Product;
import com.blss.orderservice.service.order.OrderService;
import com.blss.orderservice.service.StoreService;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DtoMapper {

    GetOrderResponse toDto(OrderService.FullOrder order);

    ProductDto toDto(Product product);

    OrderPartDto toDto(OrderService.FullOrderItem orderPart);

    InventoryProductDto toDto(StoreService.InventoryProduct storeProduct);
}
