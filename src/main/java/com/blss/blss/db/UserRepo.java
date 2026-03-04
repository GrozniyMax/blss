package com.blss.blss.db;

import com.blss.blss.domain.User;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserRepo extends CrudRepository<User, UUID> {
}
