package com.blss.bitrixjca.impl;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import javax.transaction.xa.XAResource;

@Slf4j
@Connector(
        displayName = "Bitrix24 Connector",
        vendorName = "BLSS",
        version = "1.0.0",
        eisType = "Bitrix24 REST API",
        description = "JCA connector for Bitrix24 REST API integration"
)
public class BitrixResourceAdapter implements ResourceAdapter, Serializable {

    private transient BootstrapContext bootstrapContext;

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log.info("Starting Bitrix24 Resource Adapter");
        this.bootstrapContext = ctx;
        log.info("Bitrix24 Resource Adapter started successfully");
    }

    @Override
    public void stop() {
        log.info("Stopping Bitrix24 Resource Adapter");
        bootstrapContext = null;
        log.info("Bitrix24 Resource Adapter stopped");
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        throw new UnsupportedOperationException("Bitrix24 adapter is outbound-only");
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        throw new UnsupportedOperationException("Bitrix24 adapter is outbound-only");
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) {
        return new XAResource[0];
    }
}
