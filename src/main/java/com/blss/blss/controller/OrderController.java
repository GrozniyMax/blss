package com.blss.blss.controller;

import com.blss.blss.domain.order.Status;
import com.blss.blss.dto.input.OrderCreateRequestDTO;
import com.blss.blss.dto.output.DtoMapper;
import com.blss.blss.dto.output.GetOrderResponse;
import com.blss.blss.dto.output.OrderCreationResponse;
import com.blss.blss.exception.NotFoundException;
import com.blss.blss.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Обработчик связанный с заказами
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    DtoMapper dtoMapper;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreationResponse createOrder(
            @RequestBody OrderCreateRequestDTO order
    ) {
        var creationResponse = orderService.createOrder(order.owner(), order.location(), order.productIds());

        return new OrderCreationResponse(creationResponse.orderId());
    }

    @GetMapping("/{id}")
    public GetOrderResponse getOrderById(@PathVariable UUID id) {
        var order = orderService.getOrderContentById(id);
        return dtoMapper.toDto(order);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(
            @PathVariable UUID id,
            @RequestParam Status status
    ) {
        orderService.updateStatus(id, status);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(NotFoundException ex) {
        return ex.getMessage();
    }

}
