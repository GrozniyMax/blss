package com.blss.blss.service;

import com.blss.blss.db.UserRepo;
import com.blss.blss.domain.User;
import com.blss.blss.exception.AlreadyExistsException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRegistry {

    UserRepo userRepo;

    public User register(String email) {

        userRepo.findByEmail(email)
                .ifPresent(user -> {
                    throw new AlreadyExistsException(User.class);
                });

        return userRepo.save(new User(null, email));
    }
}
