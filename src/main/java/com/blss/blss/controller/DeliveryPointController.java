package com.blss.blss.controller;

import com.blss.blss.domain.DeliveryPoint;
import com.blss.blss.dto.input.DeliveryPointCreateRequestDto;
import com.blss.blss.db.DeliveryPointRepo;
import com.blss.blss.service.camunda.CamundaProcessClient;
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

    DeliveryPointRepo deliveryPointRepo;

    CamundaProcessClient camundaProcessClient;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPoint create(@Valid @RequestBody DeliveryPointCreateRequestDto request) {
        log.info("Creating delivery point: name={}, address={}", request.name(), request.address());
        var variables = camundaProcessClient.startAndAwait("createDeliveryPointProcess", java.util.Map.of(
                "name", request.name(),
                "address", request.address()
        ), java.util.Set.of("deliveryPointId"));
        var deliveryPointId = java.util.UUID.fromString(variables.get("deliveryPointId").toString());
        var created = deliveryPointRepo.findById(deliveryPointId)
                .orElseThrow(() -> new com.blss.blss.exception.NotFoundException(DeliveryPoint.class, deliveryPointId));
        log.info("Delivery point created successfully via Camunda: id={}", created.id());
        return created;
    }
}
