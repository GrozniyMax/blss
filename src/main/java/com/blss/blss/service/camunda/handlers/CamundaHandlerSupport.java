package com.blss.blss.service.camunda.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CamundaHandlerSupport {

    private CamundaHandlerSupport() {
    }

    public static void bpmnError(ExternalTask task, ExternalTaskService service, String code, String message) {
        service.handleBpmnError(task, code, message, Map.of(
                "processErrorCode", code,
                "processError", message
        ));
    }

    public static void failure(ExternalTask task, ExternalTaskService service, Exception e) {
        service.handleFailure(task, e.getMessage(), ExceptionUtils.getStackTrace(e), 3, 30000L);
    }

    public static String validationErrors(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

    public static UUID uuid(ExternalTask task, String variable) {
        Object value = task.getVariable(variable);
        if (value == null) {
            throw new IllegalArgumentException(variable + " is required");
        }
        return UUID.fromString(value.toString());
    }

    public static Integer integer(ExternalTask task, String variable) {
        Object value = task.getVariable(variable);
        if (value == null) {
            throw new IllegalArgumentException(variable + " is required");
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    public static List<UUID> uuidList(ExternalTask task, ObjectMapper objectMapper, String variable) {
        Object value = task.getVariable(variable);
        if (value == null) {
            throw new IllegalArgumentException(variable + " is required");
        }
        try {
            JsonNode values = value instanceof List<?>
                    ? objectMapper.valueToTree(value)
                    : objectMapper.readTree(value.toString());
            if (!values.isArray()) {
                throw new IllegalArgumentException();
            }

            return objectMapper.convertValue(values, new TypeReference<List<JsonNode>>() {
                    }).stream()
                    .map(item -> item.isTextual() ? item : item.get("productId"))
                    .map(item -> {
                        if (item == null || !item.isTextual()) {
                            throw new IllegalArgumentException();
                        }
                        return UUID.fromString(item.textValue());
                    })
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    variable + " must be an array of UUID strings or objects with productId", e);
        }
    }
}
