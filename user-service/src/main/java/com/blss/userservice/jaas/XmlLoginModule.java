package com.blss.userservice.jaas;

import com.blss.userservice.security.Role;
import com.blss.userservice.xml.XmlUser.UserAccount;
import com.blss.userservice.xml.XmlUserRepository;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;
import java.util.Map;

/**
 * JAAS LoginModule that authenticates users against XML user repository.
 */
public class XmlLoginModule implements LoginModule {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XmlLoginModule.class);

    private Subject subject;
    private CallbackHandler callbackHandler;
    private XmlUserRepository userRepository;
    private boolean authenticated = false;
    private String username;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        this.userRepository = JaasBridge.get().getUserRepository();

        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }

        try {
            NameCallback nameCallback = new NameCallback("Username");
            PasswordCallback passwordCallback = new PasswordCallback("Password", false);

            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});

            username = nameCallback.getName();
            char[] passwordChars = passwordCallback.getPassword();

            if (username == null || passwordChars == null) {
                return false;
            }

            String password = new String(passwordChars);
            passwordCallback.clearPassword();

            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                UserAccount user = userOpt.get();
                if (user.isEnabled() && user.getPassword().equals(password)) {
                    authenticated = true;
                    log.debug("User authenticated: {}", username);
                    return true;
                }
            }

            log.debug("Authentication failed for user: {}", username);
            return false;

        } catch (Exception e) {
            log.error("Login failed", e);
            throw new LoginException("Authentication failed: " + e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!authenticated) {
            return false;
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        UserAccount user = userOpt.get();

        subject.getPrincipals().add(new UsernamePrincipal(username));

        if (user.getRoles() != null && user.getRoles().getRole() != null) {
            for (Role role : user.getRoles().getRole()) {
                subject.getPrincipals().add(new RolePrincipal(role.name()));
            }
        }

        log.debug("Commit successful for user: {}", username);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        authenticated = false;
        username = null;
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeIf(p -> p instanceof UsernamePrincipal);
        subject.getPrincipals().removeIf(p -> p instanceof RolePrincipal);
        authenticated = false;
        username = null;
        return true;
    }

    public static class UsernamePrincipal implements Principal {
        private final String username;

        public UsernamePrincipal(String username) {
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }

        @Override
        public String toString() {
            return "UsernamePrincipal{" + username + "}";
        }
    }

    public static class RolePrincipal implements Principal {
        private final String roleName;

        public RolePrincipal(String roleName) {
            this.roleName = roleName;
        }

        @Override
        public String getName() {
            return roleName;
        }

        @Override
        public String toString() {
            return "RolePrincipal{" + roleName + "}";
        }
    }
}
