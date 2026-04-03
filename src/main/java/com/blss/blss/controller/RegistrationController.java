package com.blss.blss.controller;

import com.blss.blss.dto.input.UserRegisterRequestDto;
import com.blss.blss.security.Role;
import com.blss.blss.xml.XmlUser;
import com.blss.blss.xml.XmlUserRepository;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для регистрации новых пользователей.
 * Создает пользователя с базовой ролью USER.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RegistrationController {

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
