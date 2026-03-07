package com.blss.blss.db;

import com.blss.blss.domain.Product;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepo extends CrudRepository<Product, UUID> {

    @Query("INSERT INTO product (name, price) VALUES (:#{#product.name}, :#{#product.price}) RETURNING *")
    Product create(Product product);

    @Query("UPDATE product SET price = :#{#product.price} WHERE id = :#{#product.id} RETURNING *")
    Product update(Product product);

    @Query("SELECT * FROM product WHERE name = :name")
    Optional<Product> findByName(String name);

    @Query("SELECT * FROM product")
    List<Product> findAll();
}
