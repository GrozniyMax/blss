package com.blss.userservice.controller;

import com.blss.userservice.xml.XmlUserRepository;
import com.blss.userservice.dto.input.UserRegisterRequestDto;
import com.blss.userservice.security.Role;
import com.blss.userservice.xml.XmlUser;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Controller for user registration.
 * Creates a user with basic USER role.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    XmlUserRepository userRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public XmlUser.UserAccount register(@Valid @RequestBody UserRegisterRequestDto request) {
        log.info("Registering new user: username={}", request.username());
        var user = userRepository.create(request.username(), request.password(), List.of(Role.USER));
        log.info("User registered successfully: username={}", request.username());
        return user;
    }
}
