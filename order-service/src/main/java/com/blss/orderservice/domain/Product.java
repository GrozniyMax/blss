package com.blss.orderservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("product")
public record Product(
        @Id
        UUID id,
        // Название товара. Уникально в рамках текущей доменной модели
        String name,
        BigDecimal price
        //Здесь могла быть ваша реклама и другая информация о продукте
) { }
