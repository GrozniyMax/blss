package com.blss.orderservice.db;

import com.blss.orderservice.domain.SoldProduct;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SoldProductRepo extends CrudRepository<SoldProduct, UUID> {

    @Query("""
            INSERT INTO sold_products (
                product_id,
                price,
                sold_at
            ) VALUES (
                :productId,
                :price,
                :soldAt
            )
            RETURNING *
            """)
    SoldProduct recordSale(UUID productId, BigDecimal price, Instant soldAt);

    @Query("""
            INSERT INTO sold_products (
                product_id,
                price,
                sold_at
            ) VALUES (
                :productId,
                :price,
                :soldAt
            )
            RETURNING *
            """)
    SoldProduct create(SoldProduct soldProduct);

    @Query("SELECT * FROM sold_products WHERE product_id = :productId ORDER BY sold_at DESC")
    List<SoldProduct> findAllByProductId(UUID productId);
}
