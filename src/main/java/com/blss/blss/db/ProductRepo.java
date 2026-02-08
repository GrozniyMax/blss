package com.blss.blss.db;

import com.blss.blss.domain.Product;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ProductRepo extends CrudRepository<Product, UUID> {

    @Query("INSERT INTO product (name, price) VALUES (:#{#product.name}, :#{#product.price}) RETURNING *")
    Product create(Product product);

    @Query("UPDATE product SET name = :#{#product.name}, price = :#{#product.price} WHERE id = :#{#product.id} RETURNING *")
    Product update(Product product);
}
