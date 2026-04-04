package com.blss.blss.controller;

import com.blss.blss.db.DeliveryPointRepo;
import com.blss.blss.domain.DeliveryPoint;
import com.blss.blss.dto.input.DeliveryPointCreateRequestDto;
import com.blss.blss.service.DeliveryPointRegistry;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blss/delivery-points")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeliveryPointController {

    DeliveryPointRegistry registry;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPoint create(@Valid @RequestBody DeliveryPointCreateRequestDto request) {
        log.info("Creating delivery point: name={}, address={}", request.name(), request.address());
        var item = new DeliveryPoint(null, request.name(), request.address());
        var created = registry.createDeliveryPoint(item);
        log.info("Delivery point created successfully: id={}", created.id());
        return created;
    }
}
