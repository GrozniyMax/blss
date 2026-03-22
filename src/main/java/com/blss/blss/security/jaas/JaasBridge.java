package com.blss.blss.security.jaas;

import com.blss.blss.xml.XmlUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bridge between Spring context and JAAS LoginModule.
 * Provides Spring-managed beans to JAAS which is instantiated outside of Spring context.
 */
@Component
@RequiredArgsConstructor
public class JaasBridge {

    private final XmlUserRepository userRepository;

    private static JaasBridge instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * Get the singleton instance of JaasBridge.
     *
     * @return the JaasBridge instance
     * @throws IllegalStateException if the bridge is not yet initialized
     */
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
