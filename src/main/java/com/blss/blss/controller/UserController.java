package com.blss.blss.controller;

import com.blss.blss.dto.input.UserCreateRequestDto;
import com.blss.blss.dto.input.UserUpdateRequestDto;
import com.blss.blss.xml.XmlUser;
import com.blss.blss.xml.XmlUserRepository;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    XmlUserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public XmlUser.UserAccount create(@Valid @RequestBody UserCreateRequestDto request) {
        return userRepository.create(request.username(), request.password(), request.roles());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<String> getAllUsers() {
        return userRepository.getAllUsernames();
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<XmlUser.UserAccount> getUser(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<XmlUser.UserAccount> update(
            @PathVariable String username,
            @Valid @RequestBody UserUpdateRequestDto request
    ) {
        return userRepository.update(username, request.password(), request.roles(), request.enabled())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String username) {
        if (!userRepository.delete(username)) {
            throw new IllegalArgumentException("User not found: " + username);
        }
    }
}
