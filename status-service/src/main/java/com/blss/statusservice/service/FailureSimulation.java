package com.blss.statusservice.service;

import org.springframework.stereotype.Component;

@Component
public class FailureSimulation {

    private boolean failInTryCatch = false;

    private boolean failInMethod = false;

    public void setFailInTryCatch(boolean failInTryCatch) {
        this.failInTryCatch = failInTryCatch;
    }

    public boolean failInTryCatch() {
        return failInTryCatch;
    }

    public boolean isFailInMethod() {
        return failInMethod;
    }

    public void setFailInMethod(boolean failInMethod) {
        this.failInMethod = failInMethod;
    }
}
