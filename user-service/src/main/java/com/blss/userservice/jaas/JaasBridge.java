package com.blss.userservice.jaas;

import com.blss.userservice.xml.XmlUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JaasBridge {

    private final XmlUserRepository userRepository;

    private static JaasBridge instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static JaasBridge get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "JaasBridge not initialized (Spring context not ready). " +
                            "Ensure this is called after Spring application context is fully started."
            );
        }
        return instance;
    }

    public XmlUserRepository getUserRepository() {
        return userRepository;
    }
}