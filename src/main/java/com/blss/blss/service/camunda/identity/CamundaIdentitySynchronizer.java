package com.blss.blss.service.camunda.identity;

import com.blss.blss.security.Role;
import com.blss.blss.xml.XmlUser;
import com.blss.blss.xml.XmlUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaIdentitySynchronizer {

    private static final String GROUP_TYPE = "WORKFLOW";
    private static final int APPLICATION_RESOURCE = 0;
    private static final int PROCESS_DEFINITION_RESOURCE = 6;
    private static final int PROCESS_INSTANCE_RESOURCE = 8;

    private static final Map<Role, Set<String>> STARTABLE_PROCESSES = startableProcesses();
    private static final Map<Role, Set<String>> COCKPIT_PROCESSES = cockpitProcesses();

    private final RestTemplate restTemplate;
    private final XmlUserRepository userRepository;

    @Value("${camunda.bpm.client.base-url}")
    private String camundaBaseUrl;

    @Value("${camunda.bpm.client.basic-auth.username}")
    private String adminUsername;

    @Value("${camunda.bpm.client.basic-auth.password}")
    private String adminPassword;

    @Value("${camunda.identity-sync.enabled:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeOnStartup() {
        synchronizeSafely();
    }

    @Scheduled(
            initialDelayString = "${camunda.identity-sync.initial-delay-ms:5000}",
            fixedDelayString = "${camunda.identity-sync.fixed-delay-ms:60000}"
    )
    public void synchronizePeriodically() {
        synchronizeSafely();
    }

    public synchronized void synchronizeAccount(XmlUser.UserAccount account) {
        if (!enabled) {
            throw new IllegalStateException("Camunda identity synchronization is disabled");
        }

        if (!account.isEnabled()) {
            deleteDisabledUser(account);
            return;
        }

        Arrays.stream(Role.values()).forEach(this::ensureGroup);
        Arrays.stream(Role.values()).forEach(this::ensureRoleAuthorizations);
        boolean created = ensureUser(account);
        if (!created) {
            updatePassword(account);
        }
        synchronizeMemberships(account);
        log.info("Synchronized newly registered user {} with Camunda", account.getUsername());
    }

    private synchronized void synchronizeSafely() {
        if (!enabled) {
            return;
        }

        try {
            synchronize();
        } catch (Exception e) {
            log.error("Camunda identity synchronization failed: {}", e.getMessage(), e);
        }
    }

    private void synchronize() {
        Arrays.stream(Role.values()).forEach(this::ensureGroup);
        Arrays.stream(Role.values()).forEach(this::ensureRoleAuthorizations);

        int synchronizedUsers = 0;
        for (XmlUser.UserAccount account : userRepository.getAllAccounts()) {
            try {
                if (!account.isEnabled()) {
                    deleteDisabledUser(account);
                    continue;
                }

                boolean created = ensureUser(account);
                if (!created) {
                    updatePassword(account);
                }
                synchronizeMemberships(account);
                synchronizedUsers++;
            } catch (Exception e) {
                log.error("Failed to synchronize XML user {} with Camunda: {}",
                        account.getUsername(), e.getMessage(), e);
            }
        }

        log.info("Synchronized {} active XML users and {} role groups with Camunda",
                synchronizedUsers, Role.values().length);
    }

    private void ensureGroup(Role role) {
        String groupId = role.name();
        if (exists("/group/" + encode(groupId))) {
            return;
        }

        post("/group/create", Map.of(
                "id", groupId,
                "name", groupId,
                "type", GROUP_TYPE
        ));
        log.info("Created Camunda group {}", groupId);
    }

    private boolean ensureUser(XmlUser.UserAccount account) {
        String userId = camundaUserId(account.getUsername());
        if (exists("/user/" + encode(userId) + "/profile")) {
            return false;
        }

        post("/user/create", Map.of(
                "profile", Map.of(
                        "id", userId,
                        "firstName", account.getUsername(),
                        "lastName", "",
                        "email", ""
                ),
                "credentials", Map.of("password", account.getPassword())
        ));
        log.info("Created Camunda user {}", userId);
        return true;
    }

    private void updatePassword(XmlUser.UserAccount account) {
        put("/user/" + encode(camundaUserId(account.getUsername())) + "/credentials", Map.of(
                "password", account.getPassword(),
                "authenticatedUserPassword", adminPassword
        ));
    }

    private void synchronizeMemberships(XmlUser.UserAccount account) {
        Set<String> expectedGroups = new HashSet<>();
        if (account.getRoles() != null && account.getRoles().getRole() != null) {
            account.getRoles().getRole().stream()
                    .map(Role::name)
                    .forEach(expectedGroups::add);
        }

        String userId = camundaUserId(account.getUsername());
        Set<String> currentGroups = getUserGroups(userId);
        for (Role role : Role.values()) {
            String groupId = role.name();
            boolean expected = expectedGroups.contains(groupId);
            boolean current = currentGroups.contains(groupId);

            if (expected && !current) {
                put("/group/" + encode(groupId) + "/members/" + encode(userId), null);
            } else if (!expected && current) {
                delete("/group/" + encode(groupId) + "/members/" + encode(userId));
            }
        }
    }

    private Set<String> getUserGroups(String userId) {
        JsonNode groups = get("/group?member=" + encode(userId));
        Set<String> result = new HashSet<>();
        if (groups != null && groups.isArray()) {
            groups.forEach(group -> result.add(group.path("id").asText()));
        }
        return result;
    }

    private void deleteDisabledUser(XmlUser.UserAccount account) {
        String userId = camundaUserId(account.getUsername());
        if (userId.equals(adminUsername)) {
            log.warn("Camunda administrator {} is disabled in XML but will not be deleted", userId);
            return;
        }
        if (exists("/user/" + encode(userId) + "/profile")) {
            delete("/user/" + encode(userId));
            log.info("Deleted disabled XML user {} from Camunda", userId);
        }
    }

    private void ensureRoleAuthorizations(Role role) {
        ensureAuthorization(
                role.name(),
                APPLICATION_RESOURCE,
                "tasklist",
                List.of("ACCESS")
        );

        Set<String> cockpitProcessKeys = COCKPIT_PROCESSES.getOrDefault(role, Set.of());
        if (!cockpitProcessKeys.isEmpty()) {
            ensureAuthorization(
                    role.name(),
                    APPLICATION_RESOURCE,
                    "cockpit",
                    List.of("ACCESS")
            );
        } else {
            deleteAuthorizations(role.name(), APPLICATION_RESOURCE, "cockpit");
        }

        Set<String> processKeys = STARTABLE_PROCESSES.getOrDefault(role, Set.of());
        if (!processKeys.isEmpty()) {
            ensureAuthorization(
                    role.name(),
                    PROCESS_INSTANCE_RESOURCE,
                    "*",
                    List.of("CREATE")
            );
        }

        for (String processKey : processKeys) {
            ensureAuthorization(
                    role.name(),
                    PROCESS_DEFINITION_RESOURCE,
                    processKey,
                    List.of("READ", "CREATE_INSTANCE")
            );
        }

        for (String processKey : cockpitProcessKeys) {
            ensureAuthorization(
                    role.name(),
                    PROCESS_DEFINITION_RESOURCE,
                    processKey,
                    List.of("READ", "READ_INSTANCE", "READ_HISTORY")
            );
        }
    }

    private void ensureAuthorization(String groupId, int resourceType, String resourceId, List<String> permissions) {
        JsonNode authorizations = get("/authorization?type=1&groupIdIn=" + encode(groupId)
                + "&resourceType=" + resourceType);

        Set<String> grantedPermissions = new HashSet<>();
        if (authorizations != null && authorizations.isArray()) {
            for (JsonNode authorization : authorizations) {
                if (resourceId.equals(authorization.path("resourceId").asText())) {
                    authorization.path("permissions").forEach(permission ->
                            grantedPermissions.add(permission.asText()));
                }
            }
        }

        List<String> missingPermissions = permissions.stream()
                .filter(permission -> !grantedPermissions.contains(permission))
                .toList();
        if (missingPermissions.isEmpty()) {
            return;
        }

        post("/authorization/create", Map.of(
                "type", 1,
                "permissions", missingPermissions,
                "groupId", groupId,
                "resourceType", resourceType,
                "resourceId", resourceId
        ));
        log.info("Granted Camunda permissions {} to group {} on resource {}",
                missingPermissions, groupId, resourceId);
    }

    private void deleteAuthorizations(String groupId, int resourceType, String resourceId) {
        JsonNode authorizations = get("/authorization?type=1&groupIdIn=" + encode(groupId)
                + "&resourceType=" + resourceType);
        if (authorizations == null || !authorizations.isArray()) {
            return;
        }

        for (JsonNode authorization : authorizations) {
            if (resourceId.equals(authorization.path("resourceId").asText())) {
                delete("/authorization/" + encode(authorization.path("id").asText()));
                log.info("Deleted Camunda authorization for group {} on resource {}",
                        groupId, resourceId);
            }
        }
    }

    private boolean exists(String path) {
        try {
            exchange(path, HttpMethod.GET, null, JsonNode.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    private JsonNode get(String path) {
        return exchange(path, HttpMethod.GET, null, JsonNode.class).getBody();
    }

    private void post(String path, Object body) {
        exchange(path, HttpMethod.POST, body, Void.class);
    }

    private void put(String path, Object body) {
        exchange(path, HttpMethod.PUT, body, Void.class);
    }

    private void delete(String path) {
        exchange(path, HttpMethod.DELETE, null, Void.class);
    }

    private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, adminPassword);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        return restTemplate.exchange(
                camundaBaseUrl + path,
                method,
                new HttpEntity<>(body, headers),
                responseType
        );
    }

    private String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private String camundaUserId(String username) {
        String userId = username.replaceAll("[^a-zA-Z0-9]", "");
        if (userId.isBlank()) {
            userId = "user" + Integer.toUnsignedString(username.hashCode());
        }
        if (!userId.equals(username)) {
            log.debug("Mapped XML username {} to Camunda user id {}", username, userId);
        }
        return userId;
    }

    private static Map<Role, Set<String>> startableProcesses() {
        Map<Role, Set<String>> result = new EnumMap<>(Role.class);
        result.put(Role.USER, Set.of("createOrderProcess"));
        result.put(Role.MANAGER, Set.of(
                "createDeliveryPointProcess",
                "createProductProcess",
                "updateProductProcess",
                "updateProductCountProcess",
                "orderStatusProcess",
                "orderPickupProcess"
        ));
        result.put(Role.CONSULTANT, Set.of("orderStatusProcess", "orderPickupProcess"));
        result.put(Role.WAREHOUSE, Set.of("markDeliveredProcess"));
        result.put(Role.ADMIN, Set.of(
                "createDeliveryPointProcess",
                "createProductProcess",
                "createOrderProcess",
                "updateProductProcess",
                "updateProductCountProcess",
                "orderStatusProcess",
                "markDeliveredProcess",
                "orderPickupProcess"
        ));
        return Map.copyOf(result);
    }

    private static Map<Role, Set<String>> cockpitProcesses() {
        Map<Role, Set<String>> result = new EnumMap<>(Role.class);
        result.put(Role.MANAGER, STARTABLE_PROCESSES.get(Role.MANAGER));
        result.put(Role.CONSULTANT, STARTABLE_PROCESSES.get(Role.CONSULTANT));
        result.put(Role.WAREHOUSE, STARTABLE_PROCESSES.get(Role.WAREHOUSE));
        result.put(Role.ADMIN, STARTABLE_PROCESSES.get(Role.ADMIN));
        return Map.copyOf(result);
    }
}
