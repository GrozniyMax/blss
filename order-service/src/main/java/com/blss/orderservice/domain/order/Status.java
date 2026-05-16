package com.blss.orderservice.domain.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    CREATED,
    PROCESSING,
    IN_DELIVERY,
    READY_FOR_PICKUP,
    CANCELED,
    DONE;

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static Status fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
