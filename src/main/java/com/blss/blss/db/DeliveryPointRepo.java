package com.blss.blss.db;

import com.blss.blss.domain.DeliveryPoint;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface DeliveryPointRepo extends CrudRepository<DeliveryPoint, UUID> {
}
