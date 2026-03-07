package com.blss.blss.db;

import com.blss.blss.domain.DeliveryPoint;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface DeliveryPointRepo extends CrudRepository<DeliveryPoint, UUID> {

    @Query("SELECT EXISTS(SELECT 1 FROM delivery_point WHERE name = :name AND address = :address)")
    boolean existsByNameAndAddress(String name, String address);
}
