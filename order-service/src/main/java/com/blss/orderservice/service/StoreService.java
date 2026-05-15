package com.blss.orderservice.service;

import com.blss.orderservice.db.ProductRepo;
import com.blss.orderservice.db.StoreRepo;
import com.blss.orderservice.domain.Product;
import com.blss.orderservice.domain.store.StoreItem;
import com.blss.orderservice.exception.AlreadyExistsException;
import com.blss.orderservice.exception.NotFoundException;
import com.blss.orderservice.exception.UpdateException;
import com.blss.orderservice.service.tx.TransactionExecutor;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoreService {

    StoreRepo storeRepo;

    ProductRepo productRepo;

    TransactionExecutor transactionExecutor;

    public UUID createProduct(Product product) {
        return createProduct(product, 1);
    }

    public UUID createProduct(Product product, Integer initialCount) {
        return transactionExecutor.inTransaction(() -> {
            if (initialCount == null || initialCount <= 0) {
                throw new IllegalArgumentException("Initial count must be positive");
            }
            productRepo.findByName(product.name()).ifPresent(p -> {
                throw new AlreadyExistsException(Product.class);
            });
            var saved = productRepo.create(product);
            storeRepo.create(new StoreItem(saved.id(), initialCount));
            return saved.id();
        });
    }

    public void updateProduct(Product product) {
        transactionExecutor.inTransaction(() -> {
            var updated = productRepo.update(product);
            if (updated == null) {
                throw new NotFoundException(Product.class, product.id());
            }
        });
    }

    public InventoryProduct getProduct(UUID productId) {
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException(Product.class, productId));
        return storeRepo.findByProductId(productId)
                .map(storeItem -> new InventoryProduct(product, storeItem.count()))
                .orElseThrow(() -> new NotFoundException(StoreItem.class, productId));
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
                .orElseThrow(() -> new NotFoundException(StoreItem.class, productId));
    }

    public void updateItemsCount(UUID productId, Integer change) {
        transactionExecutor.inTransaction(() -> {
            try {
                storeRepo.updateCount(productId, change);
            } catch (DataIntegrityViolationException e) {
                throw new UpdateException(productId, "Items count must be positive and less than 10_000_000");
            }
        });
    }

    public record InventoryProduct(
            Product product,
            Integer count
    ) { }
}
