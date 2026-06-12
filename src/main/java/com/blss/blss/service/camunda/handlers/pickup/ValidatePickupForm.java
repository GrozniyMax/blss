package com.blss.blss.service.camunda.handlers.pickup;

import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ExternalTaskSubscription("order-pickup: validate-form")
public class ValidatePickupForm implements ExternalTaskHandler {

    private static final int MAX_COMMENT_LENGTH = 500;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            List<String> errors = new ArrayList<>();
            String acceptedError = "";
            String commentError = "";
            Object acceptedValue = task.getVariable("pickupAccepted");
            Boolean accepted = acceptedValue instanceof Boolean value ? value : null;
            String comment = normalize(task.getVariable("pickupComment"));

            if (accepted == null) {
                acceptedError = "Choose whether the customer accepts the order";
                errors.add("pickupAccepted: " + acceptedError);
            }
            if (Boolean.FALSE.equals(accepted) && comment.isBlank()) {
                commentError = "Provide a rejection reason";
                errors.add("pickupComment: " + commentError);
            }
            if (comment.length() > MAX_COMMENT_LENGTH) {
                commentError = "Must not exceed " + MAX_COMMENT_LENGTH + " characters";
                errors.add("pickupComment: " + commentError);
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("formValid", errors.isEmpty());
            variables.put("formValidationErrors", String.join("; ", errors));
            variables.put("pickupAcceptedError", acceptedError);
            variables.put("pickupCommentError", commentError);
            variables.put("pickupComment", comment);
            service.complete(task, variables);
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }

    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
