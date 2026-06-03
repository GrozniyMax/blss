package com.blss.blss.service.camunda;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CamundaProcessClient {

    private final RestTemplate restTemplate;

    @Value("${camunda.bpm.client.base-url}")
    private String camundaBaseUrl;

    @Value("${camunda.bpm.client.basic-auth.username}")
    private String username;

    @Value("${camunda.bpm.client.basic-auth.password}")
    private String password;

    @Value("${camunda.process.await-timeout-seconds:90}")
    private long awaitTimeoutSeconds;

    public Map<String, Object> startAndAwait(String processKey, Map<String, Object> variables, Set<String> expectedVariables) {
        String processInstanceId = start(processKey, variables);
        return await(processInstanceId, expectedVariables, Duration.ofSeconds(awaitTimeoutSeconds));
    }

    public String start(String processKey, Map<String, Object> variables) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variables", toCamundaVariables(variables));
        request.put("withVariablesInReturn", false);

        ResponseEntity<Map> response = restTemplate.exchange(
                camundaBaseUrl + "/process-definition/key/" + processKey + "/start",
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                Map.class
        );

        Object id = response.getBody() == null ? null : response.getBody().get("id");
        if (id == null) {
            throw new IllegalStateException("Camunda did not return process instance id");
        }
        return id.toString();
    }

    private Map<String, Object> await(String processInstanceId, Set<String> expectedVariables, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> variables = readVariables(processInstanceId);
            failIfProcessError(variables);

            if (variables.keySet().containsAll(expectedVariables)) {
                return variables;
            }

            if (!isActive(processInstanceId)) {
                failIfProcessError(variables);
                if (variables.keySet().containsAll(expectedVariables)) {
                    return variables;
                }
                if (expectedVariables.isEmpty()) {
                    return variables;
                }
                throw new IllegalStateException("Camunda process " + processInstanceId
                        + " finished without expected variables: " + expectedVariables
                        + ". Available variables: " + variables.keySet());
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Camunda process", e);
            }
        }

        throw new IllegalStateException("Timed out waiting for Camunda process " + processInstanceId);
    }

    private Map<String, Object> readVariables(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        result.putAll(readRuntimeVariables(processInstanceId));
        result.putAll(readHistoryVariables(processInstanceId));
        return result;
    }

    private Map<String, Object> readRuntimeVariables(String processInstanceId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    camundaBaseUrl + "/process-instance/" + processInstanceId + "/variables",
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class
            );
            Map<String, Object> result = new HashMap<>();
            if (response.getBody() != null) {
                response.getBody().forEach((key, value) -> {
                    if (key != null && value instanceof Map<?, ?> variable) {
                        result.put(key.toString(), variable.get("value"));
                    }
                });
            }
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            return Map.of();
        } catch (HttpServerErrorException.InternalServerError e) {
            if (e.getResponseBodyAsString().contains("execution is null")) {
                return Map.of();
            }
            throw e;
        }
    }

    private Map<String, Object> readHistoryVariables(String processInstanceId) {
        ResponseEntity<List> response = restTemplate.exchange(
                camundaBaseUrl + "/history/variable-instance?processInstanceId=" + processInstanceId,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class
        );

        Map<String, Object> result = new HashMap<>();
        if (response.getBody() == null) {
            return result;
        }
        for (Object item : response.getBody()) {
            if (item instanceof Map<?, ?> variable) {
                Object name = variable.get("name");
                if (name != null) {
                    result.put(name.toString(), variable.get("value"));
                }
            }
        }
        return result;
    }

    private boolean isActive(String processInstanceId) {
        try {
            restTemplate.exchange(
                    camundaBaseUrl + "/process-instance/" + processInstanceId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class
            );
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    private void failIfProcessError(Map<String, Object> variables) {
        Object error = variables.get("processError");
        if (error != null) {
            throw new IllegalArgumentException(error.toString());
        }
    }

    private Map<String, Object> toCamundaVariables(Map<String, Object> variables) {
        Map<String, Object> result = new LinkedHashMap<>();
        variables.forEach((key, value) -> result.put(key, Map.of(
                "value", value,
                "type", typeOf(value)
        )));
        return result;
    }

    private String typeOf(Object value) {
        if (value instanceof Integer) {
            return "Integer";
        }
        if (value instanceof Long) {
            return "Long";
        }
        if (value instanceof Boolean) {
            return "Boolean";
        }
        return "String";
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        return headers;
    }
}
