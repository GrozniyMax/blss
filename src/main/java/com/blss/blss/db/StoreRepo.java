package com.blss.blss.db;

import com.blss.blss.domain.store.StoreItem;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepo extends CrudRepository<StoreItem, UUID> {

    @Query("SELECT * FROM store WHERE product_id = :productId")
    Optional<StoreItem> findByProductId(UUID productId);

    @Query("UPDATE store SET count = :#{#storeItem.count} WHERE product_id = :#{#storeItem.productId}")
    @Modifying
    void update(StoreItem storeItem);

    @Modifying
    @Query("UPDATE store SET count = count + :change WHERE product_id = :productId")
    void updateCount(UUID productId, Integer change);

    @Modifying
    @Query("INSERT INTO store (product_id, count) VALUES (:#{#storeItem.productId}, :#{#storeItem.count})")
    void create(StoreItem storeItem);

    @Modifying
    @Query("UPDATE store SET count = count - 1 WHERE product_id IN (:productIds)")
    void decrementCount(@Param("productIds") List<UUID> productIds);
}
