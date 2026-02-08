package com.blss.blss.db;

import com.blss.blss.domain.store.StoreItem;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoreRepo extends CrudRepository<StoreItem, UUID> {

    @Query("SELECT * FROM store WHERE product_id = :productId")
    StoreItem findByProductId(UUID productId);

    @Query("UPDATE store SET count = :#{#storeItem.count} WHERE product_id = :#{#storeItem.productId}")
    void update(StoreItem storeItem);

    @Query("UPDATE store SET count = count + :change WHERE product_id = :productId")
    void updateCount(UUID productId, Integer change);

    @Query("INSERT INTO store (product_id, count) VALUES (:#{#storeItem.productId}, :#{#storeItem.count})")
    void create(StoreItem storeItem);
}
