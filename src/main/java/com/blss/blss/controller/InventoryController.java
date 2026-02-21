package com.blss.blss.controller;

import com.blss.blss.domain.Product;
import com.blss.blss.dto.input.ProductCreateRequestDto;
import com.blss.blss.dto.input.ProductUpdateRequestDto;
import com.blss.blss.dto.output.DtoMapper;
import com.blss.blss.dto.output.InventoryProductDto;
import com.blss.blss.exception.NotFoundException;
import com.blss.blss.service.StoreService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {

    StoreService storeService;

    DtoMapper dtoMapper;

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryProductDto createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        var productId = storeService.createProduct(new Product(null, request.name(), request.price()), request.initialCount());
        return dtoMapper.toDto(storeService.getProduct(productId));
    }

    @PutMapping("/products/{id}")
    public InventoryProductDto updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequestDto request) {
        storeService.updateProduct(new Product(id, request.name(), request.price()));
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @PatchMapping("/products/{id}/count")
    public InventoryProductDto updateCount(@PathVariable UUID id, @RequestParam Integer change) {
        storeService.updateItemsCount(id, change);
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @GetMapping("/products/{id}")
    public InventoryProductDto getProduct(@PathVariable UUID id) {
        return dtoMapper.toDto(storeService.getProduct(id));
    }

    @GetMapping("/products")
    public List<InventoryProductDto> getAllProducts() {
        return storeService.getAllProducts().stream().map(dtoMapper::toDto).toList();
    }

    @GetMapping("/products/{id}/count")
    public Integer getProductCount(@PathVariable UUID id) {
        return storeService.getCount(id);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(NotFoundException ex) {
        return ex.getMessage();
    }

}
