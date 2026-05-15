package com.blss.statusservice.service;

import org.springframework.stereotype.Component;

@Component
public class FailureSimulation {

    private boolean shouldFail = false;

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public boolean shouldFail() {
        return shouldFail;
    }
}
