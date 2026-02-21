package com.blss.blss.service;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.db.StoreRepo;
import com.blss.blss.domain.Product;
import com.blss.blss.domain.store.StoreItem;
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
        var saved = productRepo.create(product);
        storeRepo.create(new StoreItem(saved.id(), initialCount));
        return saved.id();
    }

    public void updateProduct(Product product) {
        var updated = productRepo.update(product);
        if (updated == null) {
            throw new NotFoundException(Product.class, "Product not found");
        }
    }

    public InventoryProduct getProduct(UUID productId) {
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException(Product.class, "Product not found"));
        var storeItem = storeRepo.findByProductId(productId);
        if (storeItem == null) {
            throw new NotFoundException(StoreItem.class, "Store item not found");
        }
        return new InventoryProduct(product, storeItem.count());
    }

    public List<InventoryProduct> getAllProducts() {
        Map<UUID, Integer> storeByProduct = StreamSupport.stream(storeRepo.findAll().spliterator(), false)
                .collect(Collectors.toMap(StoreItem::productId, StoreItem::count));

        return StreamSupport.stream(productRepo.findAll().spliterator(), false)
                .map(product -> new InventoryProduct(product, storeByProduct.getOrDefault(product.id(), 0)))
                .toList();
    }

    public Integer getCount(UUID productId) {
        var storeItem = storeRepo.findByProductId(productId);
        if (storeItem == null) {
            throw new NotFoundException(StoreItem.class, "Store item not found");
        }
        return storeItem.count();
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
