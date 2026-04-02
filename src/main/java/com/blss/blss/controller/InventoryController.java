package com.blss.blss.controller;

import com.blss.blss.domain.Product;
import com.blss.blss.dto.input.ProductCreateRequestDto;
import com.blss.blss.dto.input.ProductUpdateRequestDto;
import com.blss.blss.dto.output.DtoMapper;
import com.blss.blss.dto.output.InventoryProductDto;
import com.blss.blss.service.StoreService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InventoryController {

    StoreService storeService;

    DtoMapper dtoMapper;

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public InventoryProductDto createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        log.info("Creating product: name={}, price={}, initialCount={}", request.name(), request.price(), request.initialCount());
        var productId = storeService.createProduct(new Product(null, request.name(), request.price()), request.initialCount());
        log.info("Product created successfully: id={}", productId);
        return dtoMapper.toDto(storeService.getProduct(productId));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public InventoryProductDto updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequestDto request) {
        log.info("Updating product: id={}, name={}, price={}", id, request.name(), request.price());
        storeService.updateProduct(new Product(id, request.name(), request.price()));
        log.info("Product updated successfully: id={}", id);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @PatchMapping("/products/{id}/count")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public InventoryProductDto updateCount(@PathVariable UUID id, @RequestParam Integer change) {
        log.info("Updating product count: id={}, change={}", id, change);
        storeService.updateItemsCount(id, change);
        log.info("Product count updated successfully: id={}", id);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public InventoryProductDto getProduct(@PathVariable UUID id) {
        log.info("Getting product: id={}", id);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<InventoryProductDto> getAllProducts() {
        log.info("Getting all products");
        return storeService.getAllProducts().stream().map(dtoMapper::toDto).toList();
    }

    @GetMapping("/products/{id}/count")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Integer getProductCount(@PathVariable UUID id) {
        log.info("Getting product count: id={}", id);
        return storeService.getCount(id);
    }

}
