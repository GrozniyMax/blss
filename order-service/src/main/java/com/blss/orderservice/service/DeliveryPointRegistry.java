package com.blss.orderservice.service;

import com.blss.orderservice.db.DeliveryPointRepo;
import com.blss.orderservice.domain.DeliveryPoint;
import com.blss.orderservice.exception.AlreadyExistsException;
import com.blss.orderservice.service.tx.TransactionExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryPointRegistry {

    DeliveryPointRepo deliveryPointRepo;

    TransactionExecutor transactionExecutor;


    public DeliveryPoint createDeliveryPoint(DeliveryPoint point) {
        return transactionExecutor.inTransaction(() -> {
            if (deliveryPointRepo.existsByNameAndAddress(point.name(), point.address())) {
                throw new AlreadyExistsException(DeliveryPoint.class);
            }
            return deliveryPointRepo.save(point);
        });
    }


}
