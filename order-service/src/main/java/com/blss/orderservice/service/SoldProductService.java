package com.blss.orderservice.service;

import com.blss.orderservice.db.SoldProductRepo;
import com.blss.orderservice.domain.SoldProduct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SoldProductService {

    SoldProductRepo soldProductRepo;

    /**
     * Records a product sale.
     *
     * @param productId the UUID of the sold product
     * @param price     the price at which the product was sold
     * @param soldAt    the timestamp of the sale
     * @return the recorded SoldProduct entity
     */
    public SoldProduct recordSale(UUID productId, BigDecimal price, Instant soldAt) {
        return soldProductRepo.recordSale(productId, price, soldAt);
    }

    /**
     * Records multiple product sales at once.
     *
     * @param productIds the UUIDs of the sold products
     * @param prices     the prices at which the products were sold (must match productIds size)
     * @param soldAt     the timestamp of the sale
     * @return list of recorded SoldProduct entities
     */
    public List<SoldProduct> recordSales(List<UUID> productIds, List<BigDecimal> prices, Instant soldAt) {
        if (productIds.size() != prices.size()) {
            throw new IllegalArgumentException("Product IDs and prices must have the same size");
        }

        return productIds.stream()
                .map(id -> soldProductRepo.recordSale(id, prices.get(productIds.indexOf(id)), soldAt))
                .toList();
    }

    /**
     * Gets all sales for a specific product.
     *
     * @param productId the UUID of the product
     * @return list of SoldProduct entities sorted by sale date (newest first)
     */
    public List<SoldProduct> getSalesByProductId(UUID productId) {
        return soldProductRepo.findAllByProductId(productId);
    }
}
