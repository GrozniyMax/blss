package com.blss.blss.security.jaas;

import com.blss.blss.security.Role;
import com.blss.blss.xml.XmlUser;
import com.blss.blss.xml.XmlUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JAAS LoginModule implementation for XML-based user authentication.
 * Uses plain text password comparison and loads users from XML configuration.
 */
public class XmlLoginModule implements LoginModule {

    private static final Logger log = LoggerFactory.getLogger(XmlLoginModule.class);

    private Subject subject;
    private CallbackHandler callbackHandler;

    // Список добавленных Principal для последующего удаления при logout
    private final List<Principal> addedPrincipals = new ArrayList<>();

    // Временные переменные для процесса аутентификации
    private char[] password;
    private XmlUser.UserAccount userAccount;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        addedPrincipals.clear();
        this.password = null;
        this.userAccount = null;

        log.debug("XmlLoginModule initialized with subject: {}", subject);
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }

        try {
            // Получаем username и password из callback handler
            NameCallback nameCallback = new NameCallback("Username: ");
            PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);

            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});

            String username = nameCallback.getName();
            this.password = passwordCallback.getPassword();

            if (username == null || username.trim().isEmpty()) {
                throw new LoginException("Username is required");
            }

            if (this.password == null || this.password.length == 0) {
                throw new LoginException("Password is required");
            }

            // Получаем доступ к Spring beans через JaasBridge
            JaasBridge bridge = JaasBridge.get();
            XmlUserRepository userRepository = bridge.getUserRepository();

            // Аутентификация против XML хранилища пользователей
            Optional<XmlUser.UserAccount> accountOpt = userRepository.findByUsername(username.trim());

            if (accountOpt.isEmpty()) {
                log.warn("Authentication failed: user '{}' not found", username);
                throw new LoginException("Invalid username or password");
            }

            this.userAccount = accountOpt.get();

            // Проверяем, активен ли пользователь
            if (!this.userAccount.isEnabled()) {
                log.warn("Authentication failed: user '{}' is disabled", username);
                throw new LoginException("User account is disabled");
            }

            // Проверяем пароль (plain text comparison)
            String storedPassword = this.userAccount.getPassword();
            if (!storedPassword.equals(new String(this.password))) {
                log.warn("Authentication failed: invalid password for user '{}'", username);
                throw new LoginException("Invalid username or password");
            }

            // Формируем список Principal для добавления в Subject
            // Username principal
            addedPrincipals.add(new UsernamePrincipal(username.trim()));

            // Role principals
            if (userAccount.getRoles() != null && userAccount.getRoles().getRole() != null) {
                for (Role role : userAccount.getRoles().getRole()) {
                    addedPrincipals.add(new RolePrincipal(role.name()));
                    log.debug("Added role principal: {}", role);
                }
            }

            log.info("User '{}' authenticated successfully with {} roles",
                    username,
                    userAccount.getRoles() != null ? userAccount.getRoles().getRole().size() : 0);
            return true;

        } catch (IOException | UnsupportedCallbackException e) {
            log.error("Authentication error", e);
            throw new LoginException("Authentication failed: " + e.getMessage());
        } finally {
            // Очищаем пароль из памяти
            if (this.password != null) {
                java.util.Arrays.fill(this.password, ' ');
                this.password = null;
            }
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (addedPrincipals.isEmpty()) {
            return false;
        }

        if (subject == null) {
            throw new LoginException("Subject is null");
        }

        // Добавляем все Principal в Subject
        for (Principal principal : addedPrincipals) {
            if (!subject.getPrincipals().contains(principal)) {
                subject.getPrincipals().add(principal);
            }
        }

        log.info("Commit successful: added {} principals to subject", addedPrincipals.size());

        // Очищаем временные переменные
        this.userAccount = null;

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        logout();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        if (subject != null) {
            for (Principal principal : addedPrincipals) {
                subject.getPrincipals().remove(principal);
            }
        }

        addedPrincipals.clear();
        this.userAccount = null;

        log.debug("Logout successful");
        return true;
    }

    /**
     * Principal representing a username.
     */
    public static class UsernamePrincipal implements Principal {
        private final String name;

        public UsernamePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UsernamePrincipal)) return false;
            UsernamePrincipal that = (UsernamePrincipal) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "UsernamePrincipal{name='" + name + "'}";
        }
    }

    /**
     * Principal representing a role.
     */
    public static class RolePrincipal implements Principal {
        private final String name;

        public RolePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RolePrincipal)) return false;
            RolePrincipal that = (RolePrincipal) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "RolePrincipal{name='" + name + "'}";
        }
    }
}
