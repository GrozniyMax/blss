package com.blss.blss.controller;

import com.blss.blss.db.DeliveryPointRepo;
import com.blss.blss.domain.DeliveryPoint;
import com.blss.blss.dto.input.DeliveryPointCreateRequestDto;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delivery-points")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryPointController {

    DeliveryPointRepo deliveryPointRepo;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPoint create(@Valid @RequestBody DeliveryPointCreateRequestDto request) {
        return deliveryPointRepo.save(new DeliveryPoint(null, request.name(), request.address()));
    }
}
