package com.blss.blss.controller;

import com.blss.blss.dto.input.ProductCreateRequestDto;
import com.blss.blss.dto.input.ProductUpdateRequestDto;
import com.blss.blss.dto.output.DtoMapper;
import com.blss.blss.dto.output.InventoryProductDto;
import com.blss.blss.service.StoreService;
import com.blss.blss.service.camunda.CamundaProcessClient;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/blss/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InventoryController {

    StoreService storeService;

    CamundaProcessClient camundaProcessClient;

    DtoMapper dtoMapper;

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryProductDto createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        log.info("Creating product: name={}, price={}, initialCount={}", request.name(), request.price(), request.initialCount());
        var variables = camundaProcessClient.startAndAwait("createProductProcess", java.util.Map.of(
                "productName", request.name(),
                "price", request.price().toPlainString(),
                "initialCount", request.initialCount()
        ), java.util.Set.of("productId"));
        var productId = UUID.fromString(variables.get("productId").toString());
        log.info("Product created successfully via Camunda: id={}", productId);
        return dtoMapper.toDto(storeService.getProduct(productId));
    }

    @PutMapping("/products/{id}")
    public InventoryProductDto updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequestDto request) {
        log.info("Updating product: id={}, name={}, price={}", id, request.name(), request.price());
        camundaProcessClient.startAndAwait("updateProductProcess", java.util.Map.of(
                "productId", id.toString(),
                "name", request.name(),
                "price", request.price().toPlainString()
        ), java.util.Set.of("processSuccess"));
        log.info("Product updated successfully via Camunda: id={}", id);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @PatchMapping("/products/{id}/count")
    public InventoryProductDto updateCount(@PathVariable UUID id, @RequestParam Integer change) {
        log.info("Updating product count: id={}, change={}", id, change);
        camundaProcessClient.startAndAwait("updateProductCountProcess", java.util.Map.of(
                "productId", id.toString(),
                "change", change
        ), java.util.Set.of("processSuccess"));
        log.info("Product count updated successfully via Camunda: id={}", id);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @GetMapping("/products/{id}")
    public InventoryProductDto getProduct(@PathVariable UUID id) {
        log.info("Getting product: id={}", id);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @GetMapping("/products")
    public List<InventoryProductDto> getAllProducts() {
        log.info("Getting all products");
        return storeService.getAllProducts().stream().map(dtoMapper::toDto).toList();
    }

    @GetMapping("/products/{id}/count")
    public Integer getProductCount(@PathVariable UUID id) {
        log.info("Getting product count: id={}", id);
        return storeService.getCount(id);
    }

}
