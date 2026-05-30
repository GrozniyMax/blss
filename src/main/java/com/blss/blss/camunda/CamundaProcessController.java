package com.blss.blss.camunda;

import com.blss.blss.dto.input.OrderCreateRequestDTO;
import com.blss.blss.dto.input.ProductCreateRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/camunda/processes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CamundaProcessController {

    CamundaProcessClient processClient;

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CamundaProcessClient.ProcessStartResponse startProductCreation(
            @Valid @RequestBody ProductCreateRequestDto request
    ) {
        return processClient.startProductCreation(Map.of(
                "name", CamundaVariable.string(request.name()),
                "price", CamundaVariable.string(request.price()),
                "initialCount", CamundaVariable.integer(request.initialCount())
        ));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CamundaProcessClient.ProcessStartResponse startOrderCreation(
            @Valid @RequestBody OrderCreateRequestDTO request
    ) {
        return processClient.startOrderCreation(Map.of(
                "owner", CamundaVariable.string(request.owner()),
                "location", CamundaVariable.string(request.location()),
                "productIds", CamundaVariable.json(request.productIds().stream().map(UUID::toString).toList())
        ));
    }

    @PostMapping("/pickup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CamundaProcessClient.ProcessStartResponse startOrderPickup(
            @Valid @RequestBody OrderPickupProcessRequest request
    ) {
        return processClient.startOrderPickup(Map.of(
                "orderId", CamundaVariable.string(request.orderId()),
                "consultantId", CamundaVariable.string(request.consultantId())
        ));
    }

    public record OrderPickupProcessRequest(
            @NotNull UUID orderId,
            @NotNull String consultantId
    ) {
    }
}
