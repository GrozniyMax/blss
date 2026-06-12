package com.blss.blss.service.camunda.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public final class CamundaHandlerSupport {

    private CamundaHandlerSupport() {
    }

    public static void bpmnError(ExternalTask task, ExternalTaskService service, String code, String message) {
        String processInstanceId = task.getProcessInstanceId();
        String activityId = task.getActivityId();
        log.error("BPMN Error in process instance {}: activity={}, errorCode={}, message={}",
                processInstanceId, activityId, code, message);
        
        service.handleBpmnError(task, code, message, Map.of(
                "processErrorCode", code,
                "processError", message
        ));
    }

    public static void failure(ExternalTask task, ExternalTaskService service, Exception e) {
        String processInstanceId = task.getProcessInstanceId();
        String activityId = task.getActivityId();
        String topicName = task.getTopicName();
        
        log.error("Task failure: topic={}, processInstanceId={}, activity={}, error={}, stackTrace={}",
                topicName, processInstanceId, activityId, e.getMessage(), ExceptionUtils.getStackTrace(e));
        
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
            List<String> ids;
            if (value instanceof List<?> list) {
                ids = list.stream().map(Object::toString).toList();
            } else {
                ids = objectMapper.readValue(value.toString(), new TypeReference<>() {
                });
            }
            return ids.stream().map(UUID::fromString).toList();
        } catch (Exception e) {
            throw new IllegalArgumentException(variable + " must be a JSON array of UUID strings", e);
        }
    }
}
