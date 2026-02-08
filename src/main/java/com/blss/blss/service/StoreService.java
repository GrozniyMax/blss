package com.blss.blss.service;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.db.StoreRepo;
import com.blss.blss.domain.Product;
import com.blss.blss.domain.store.StoreItem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        var saved = productRepo.create(product);
        storeRepo.create(new StoreItem(saved.id(), 0));
        return saved.id();
    }

    public void updateProduct(Product product) {
        productRepo.update(product);
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
}
