package com.blss.blss.service.camunda.deploy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BpmnAutoDeployer {

    private final RestTemplate restTemplate;

    @Value("${camunda.bpm.client.base-url}")
    private String camundaBaseUrl;

    @Value("${camunda.bpm.client.basic-auth.username}")
    private String username;

    @Value("${camunda.bpm.client.basic-auth.password}")
    private String password;

    @EventListener(ApplicationReadyEvent.class)
    public void deploy() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] bpmnResources = resolver.getResources("classpath:processes/*.bpmn");
        Resource[] formResources = resolver.getResources("classpath:forms/*.form");
        Resource[] dmnResources = resolver.getResources("classpath:decisions/*.dmn");

        if (bpmnResources.length == 0 && formResources.length == 0 && dmnResources.length == 0) {
            log.warn("No BPMN, DMN or Camunda form files found in classpath:processes/, classpath:decisions/ and classpath:forms/");
            return;
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("deployment-name", "blss-auto-deploy");
        body.add("deploy-changed-only", "true");
        body.add("deployment-source", "spring-boot-app");

        addResources(body, bpmnResources);
        addResources(body, dmnResources);
        addResources(body, formResources);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(username, password);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    camundaBaseUrl + "/deployment/create",
                    new HttpEntity<>(body, headers),
                    String.class
            );
            log.info("BPMN deployed: {}", response.getBody());
        } catch (Exception e) {
            log.error("BPMN deploy failed", e);
        }
    }

    private void addResources(MultiValueMap<String, Object> body, Resource[] resources) throws IOException {
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            log.info("Adding {} to deployment", filename);
            body.add(filename, new ByteArrayResource(resource.getInputStream().readAllBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
        }
    }
}
