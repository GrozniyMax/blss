package com.blss.blss.camunda;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CamundaProcessClient {

    RestClient camundaRestClient;
    CamundaProperties properties;

    public ProcessStartResponse startProductCreation(Map<String, CamundaVariable> variables) {
        return startByKey(properties.processes().productCreationKey(), variables);
    }

    public ProcessStartResponse startOrderCreation(Map<String, CamundaVariable> variables) {
        return startByKey(properties.processes().orderCreationKey(), variables);
    }

    public ProcessStartResponse startOrderPickup(Map<String, CamundaVariable> variables) {
        return startByKey(properties.processes().orderPickupKey(), variables);
    }

    private ProcessStartResponse startByKey(String processDefinitionKey, Map<String, CamundaVariable> variables) {
        return camundaRestClient.post()
                .uri("/process-definition/key/{key}/start", processDefinitionKey)
                .body(new ProcessStartRequest(variables, true))
                .retrieve()
                .body(ProcessStartResponse.class);
    }

    private record ProcessStartRequest(
            Map<String, CamundaVariable> variables,
            boolean withVariablesInReturn
    ) {
    }

    public record ProcessStartResponse(
            String id,
            String definitionId,
            String businessKey,
            String caseInstanceId,
            boolean ended,
            boolean suspended,
            Map<String, CamundaVariable> variables
    ) {
    }
}
