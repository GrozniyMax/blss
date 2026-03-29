package com.blss.blss.service;

import com.blss.blss.security.Role;
import com.blss.blss.xml.XmlUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRegistry {

    XmlUserRepository userRepository;

    /**
     * Register a new user with username and password.
     */
    public void register(String username, String password, List<Role> roles) {
        userRepository.create(username, password, roles);
    }

    /**
     * Check if user exists by username.
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Update user password, roles, or enabled status.
     */
    public void update(String username, String newPassword, List<Role> newRoles, Boolean enabled) {
        userRepository.update(username, newPassword, newRoles, enabled);
    }

    /**
     * Delete user by username.
     */
    public void delete(String username) {
        userRepository.delete(username);
    }
}
