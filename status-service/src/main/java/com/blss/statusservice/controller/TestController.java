package com.blss.statusservice.controller;

import com.blss.statusservice.service.FailureSimulation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestController {

    private FailureSimulation failureSimulation;

    @GetMapping("/fail")
    public String fail(@RequestParam boolean shouldFail) {
        failureSimulation.setShouldFail(shouldFail);
        return "Failure simulation set to " + (shouldFail ? "enabled" : "disabled");
    }
}
