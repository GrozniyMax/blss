package com.blss.blss.controller;

import com.blss.blss.dto.input.OrderItemDeliveredDto;
import com.blss.blss.service.StorageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Сервис отвечающий за обработку запросов от сотрудника ПВЗ
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PVZController {

    StorageService storageService;

    @PostMapping("/mark-delivered")
    public void markDelivered(@Valid @RequestBody OrderItemDeliveredDto dto) {
        storageService.updateYacheyka(dto.itemId(), dto.yacheyka());
    }


}
