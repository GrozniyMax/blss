package com.blss.blss.xml;

import com.blss.blss.config.SecurityUsersProperties;
import com.blss.blss.security.Role;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Repository for loading and caching user accounts from XML configuration file.
 * Loads users into cache at startup and persists changes to file on shutdown and every minute.
 */
@Slf4j
@Repository
public class XmlUserRepository {

    private final Map<String, XmlUser.UserAccount> usersCache = new ConcurrentHashMap<>();
    private final XmlMapper xmlMapper = new XmlMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Path xmlFilePath;
    private volatile boolean initialized = false;

    public XmlUserRepository(SecurityUsersProperties properties) {
        xmlFilePath = Paths.get(properties.getXmlPath());
    }

    @PostConstruct
    public void init() {
        loadUsers();
        initialized = true;
        schedulePeriodicSave();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (initialized && !usersCache.isEmpty()) {
            try {
                saveAll();
                log.info("Users cache persisted to XML file on shutdown");
            } catch (Exception e) {
                log.error("Failed to save users on shutdown: {}", e.getMessage(), e);
            }
        }
    }

    private void schedulePeriodicSave() {
        scheduler.scheduleAtFixedRate(
                () -> {
                    if (!usersCache.isEmpty()) {
                        try {
                            saveAll();
                            log.debug("Users cache auto-saved to XML file");
                        } catch (Exception e) {
                            log.error("Failed to auto-save users cache: {}", e.getMessage(), e);
                        }
                    }
                },
                1,
                1,
                TimeUnit.MINUTES
        );
        log.info("Scheduled periodic users cache save every 1 minute");
    }

    /**
     * Loads user accounts from XML file into cache.
     */
    public void loadUsers() {
        try {
            XmlUser xmlUser = readXmlFile();
            usersCache.clear();

            if (xmlUser.getUsers() != null) {
                for (XmlUser.UserAccount account : xmlUser.getUsers()) {
                    usersCache.put(account.getUsername().toLowerCase(), account);
                }
            }

            log.info("Loaded {} users from XML configuration", usersCache.size());
        } catch (Exception e) {
            log.error("Failed to load users from XML: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize user repository", e);
        }
    }

    private XmlUser readXmlFile() throws Exception {
        if (xmlFilePath == null || !Files.exists(xmlFilePath)) {
            try (InputStream inputStream = new FileInputStream(xmlFilePath.toFile())) {
                return xmlMapper.readValue(inputStream, XmlUser.class);
            }
        }
        return xmlMapper.readValue(xmlFilePath.toFile(), XmlUser.class);
    }

    private void writeXmlFile(XmlUser xmlUser) throws Exception {
        File parentDir = xmlFilePath.getParent().toFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        xmlMapper.writeValue(xmlFilePath.toFile(), xmlUser);
        log.info("Saved {} users to XML file", xmlUser.getUsers() != null ? xmlUser.getUsers().size() : 0);
    }

    /**
     * Finds a user account by username.
     *
     * @param username the username to search for
     * @return Optional containing the user account if found
     */
    public Optional<XmlUser.UserAccount> findByUsername(String username) {
        return Optional.ofNullable(usersCache.get(username.toLowerCase()));
    }

    /**
     * Checks if a user exists by username.
     *
     * @param username the username to check
     * @return true if user exists
     */
    public boolean existsByUsername(String username) {
        return usersCache.containsKey(username.toLowerCase());
    }

    /**
     * Gets all usernames.
     *
     * @return list of all usernames
     */
    public List<String> getAllUsernames() {
        return Collections.unmodifiableList(
            usersCache.keySet().stream().collect(Collectors.toList())
        );
    }

    public List<XmlUser.UserAccount> getAllAccounts() {
        return List.copyOf(usersCache.values());
    }

    /**
     * Creates a new user account.
     *
     * @param username the username
     * @param password the plain text password
     * @param roles the list of roles
     * @return the created user account
     * @throws IllegalArgumentException if user already exists
     */
    public XmlUser.UserAccount create(String username, String password, List<Role> roles) {
        String normalizedUsername = username.toLowerCase();

        if (usersCache.containsKey(normalizedUsername)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }

        XmlUser.UserAccount newAccount = new XmlUser.UserAccount();
        newAccount.setUsername(username);
        newAccount.setPassword(password);
        newAccount.setEnabled(true);

        XmlUser.Roles userRoles = new XmlUser.Roles();
        userRoles.setRole(roles != null ? roles : new ArrayList<>());
        newAccount.setRoles(userRoles);

        usersCache.put(normalizedUsername, newAccount);

        log.info("Created user: {}", username);
        return newAccount;
    }

    /**
     * Updates an existing user account.
     *
     * @param username the username to update
     * @param newPassword the new plain text password (null to keep unchanged)
     * @param newRoles the new list of roles (null to keep unchanged)
     * @param enabled the new enabled status (null to keep unchanged)
     * @return the updated user account
     * @throws IllegalArgumentException if user does not exist
     */
    public Optional<XmlUser.UserAccount> update(String username, String newPassword, List<Role> newRoles, Boolean enabled) {
        String normalizedUsername = username.toLowerCase();
        XmlUser.UserAccount existingAccount = usersCache.get(normalizedUsername);

        if (existingAccount == null) {
            return Optional.empty();
        }

        if (newPassword != null) {
            existingAccount.setPassword(newPassword);
        }

        if (newRoles != null) {
            XmlUser.Roles userRoles = new XmlUser.Roles();
            userRoles.setRole(newRoles);
            existingAccount.setRoles(userRoles);
        }

        if (enabled != null) {
            existingAccount.setEnabled(enabled);
        }

        log.info("Updated user: {}", username);
        return Optional.of(existingAccount);
    }

    /**
     * Deletes a user account.
     *
     * @param username the username to delete
     * @return true if user was deleted, false if user did not exist
     */
    public boolean delete(String username) {
        String normalizedUsername = username.toLowerCase();
        XmlUser.UserAccount removed = usersCache.remove(normalizedUsername);
        
        if (removed == null) {
            return false;
        }

        log.info("Deleted user: {}", username);
        return true;
    }

    private void saveAll() throws Exception {
        List<XmlUser.UserAccount> allUsers = new ArrayList<>(usersCache.values());
        XmlUser xmlUser = new XmlUser();
        xmlUser.setUsers(allUsers);
        writeXmlFile(xmlUser);
    }

    /**
     * Reloads users from XML file (useful for runtime updates).
     */
    public void reload() {
        loadUsers();
    }
}
