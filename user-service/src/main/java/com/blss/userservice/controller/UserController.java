package com.blss.userservice.controller;

import com.blss.userservice.dto.input.UserCreateRequestDto;
import com.blss.userservice.dto.input.UserUpdateRequestDto;
import com.blss.userservice.xml.XmlUser;
import com.blss.userservice.xml.XmlUserRepository;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    XmlUserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public XmlUser.UserAccount create(@Valid @RequestBody UserCreateRequestDto request) {
        log.info("Creating user: username={}, roles={}", request.username(), request.roles());
        var user = userRepository.create(request.username(), request.password(), request.roles());
        log.info("User created successfully: username={}", request.username());
        return user;
    }

    @GetMapping
    public List<String> getAllUsers() {
        log.info("Getting all users");
        return userRepository.getAllUsernames();
    }

    @GetMapping("/{username}")
    public ResponseEntity<XmlUser.UserAccount> getUser(@PathVariable String username) {
        log.info("Getting user: username={}", username);
        return userRepository.findByUsername(username)
                .map(user -> {
                    log.info("User found: username={}", username);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{username}")
    public ResponseEntity<XmlUser.UserAccount> update(
            @PathVariable String username,
            @Valid @RequestBody UserUpdateRequestDto request
    ) {
        log.info("Updating user: username={}", username);
        return userRepository.update(username, request.password(), request.roles(), request.enabled())
                .map(user -> {
                    log.info("User updated successfully: username={}", username);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String username) {
        log.info("Deleting user: username={}", username);
        if (!userRepository.delete(username)) {
            log.warn("User not found for deletion: username={}", username);
            throw new IllegalArgumentException("User not found: " + username);
        }
        log.info("User deleted successfully: username={}", username);
    }
}
