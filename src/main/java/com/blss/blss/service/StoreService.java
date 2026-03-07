package com.blss.blss.service;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.db.StoreRepo;
import com.blss.blss.domain.Product;
import com.blss.blss.domain.store.StoreItem;
import com.blss.blss.exception.AlreadyExistsException;
import com.blss.blss.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Сервис отвечающий за логику
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoreService {

    StoreRepo storeRepo;

    ProductRepo productRepo;

    /**
     * Создание товара в магазине (на главном складе)
     */
    public UUID createProduct(Product product) {
        return createProduct(product, 1);
    }

    public UUID createProduct(Product product, Integer initialCount) {
        if (initialCount == null || initialCount <= 0) {
            throw new IllegalArgumentException("Initial count must be positive");
        }
        productRepo.findByName(product.name()).ifPresent(p -> {
            throw new AlreadyExistsException(Product.class);
        });
        var saved = productRepo.create(product);
        storeRepo.create(new StoreItem(saved.id(), initialCount));
        return saved.id();
    }

    public void updateProduct(Product product) {
        var updated = productRepo.update(product);
        if (updated == null) {
            throw new NotFoundException(Product.class);
        }
    }

    public InventoryProduct getProduct(UUID productId) {
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException(Product.class));
        return storeRepo.findByProductId(productId)
                .map(storeItem -> new InventoryProduct(product, storeItem.count()))
                .orElseThrow(() -> new NotFoundException(StoreItem.class));
    }

    public List<InventoryProduct> getAllProducts() {
        Map<UUID, Integer> storeByProduct = StreamSupport.stream(storeRepo.findAll().spliterator(), false)
                .collect(Collectors.toMap(StoreItem::productId, StoreItem::count));

        return productRepo.findAll().stream()
                .map(product -> new InventoryProduct(product, storeByProduct.getOrDefault(product.id(), 0)))
                .toList();
    }

    public Integer getCount(UUID productId) {
        return storeRepo.findByProductId(productId)
                .map(StoreItem::count)
                .orElseThrow(() -> new NotFoundException(StoreItem.class));
    }

    /**
     * Обновление количества товара на складе
     * @param productId товар
     * @param change количество для изменения (может быть любого знака)
     */
    public void updateItemsCount(UUID productId, Integer change) {
        try {
            storeRepo.updateCount(productId, change);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Items count must be positive");
        }
    }

    public record InventoryProduct(
            Product product,
            Integer count
    ) { }
}
