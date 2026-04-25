package com.blss.orderservice.controller;

import com.blss.orderservice.dto.input.OrderItemDeliveredDto;
import com.blss.orderservice.service.StorageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for PVZ (pickup point) operations.
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PVZController {

    StorageService storageService;

    @PostMapping("/mark-delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markDelivered(@Valid @RequestBody OrderItemDeliveredDto dto) {
        log.info("Marking order item as delivered: itemId={}, yacheyka={}", dto.itemId(), dto.yacheyka());
        storageService.updateYacheyka(dto.itemId(), dto.yacheyka());
        log.info("Order item marked as delivered: itemId={}", dto.itemId());
    }


}
