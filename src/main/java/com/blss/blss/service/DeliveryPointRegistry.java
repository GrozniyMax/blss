package com.blss.blss.service;

import com.blss.blss.db.DeliveryPointRepo;
import com.blss.blss.domain.DeliveryPoint;
import com.blss.blss.exception.AlreadyExistsException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryPointRegistry {

    DeliveryPointRepo deliveryPointRepo;


    public DeliveryPoint createDeliveryPoint(DeliveryPoint point){
        if(deliveryPointRepo.existsByNameAndAddress(point.name(), point.address())){
            throw new AlreadyExistsException(DeliveryPoint.class);
        }
        return deliveryPointRepo.save(point);
    }


}
