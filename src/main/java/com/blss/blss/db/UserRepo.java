package com.blss.blss.db;

import com.blss.blss.domain.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends CrudRepository<User, UUID> {

    @Query("SELECT * FROM users WHERE email = :email")
    Optional<User> findByEmail(String email);
}
