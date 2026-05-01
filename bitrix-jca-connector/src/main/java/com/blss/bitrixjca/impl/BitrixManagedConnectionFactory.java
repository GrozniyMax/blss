package com.blss.bitrixjca.impl;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * Managed connection factory for creating Bitrix24 connections.
 */
@Slf4j
@Getter
@Setter
public class BitrixManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    private String webhookUrl;
    private Integer documentTemplateId = 0;
    private Boolean uploadToCrmFallback = true;
    private String dealMethod = "crm.deal.add.json";
    private Long assignedById = 0L;
    private Integer categoryId = 0;

    private transient ConnectionManager connectionManager;
    private transient PrintWriter logWriter;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        log.debug("Creating connection factory (standalone mode)");
        return new BitrixConnectionFactoryImpl(this, null);
    }

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) throws ResourceException {
        log.debug("Creating connection factory (managed mode)");
        return new BitrixConnectionFactoryImpl(this, connectionManager);
    }

    @Override
    public BitrixManagedConnection createManagedConnection(
            Subject subject,
            ConnectionRequestInfo connectionRequestInfo
    ) throws ResourceException {
        log.debug("Creating physical connection to Bitrix24 REST API");
        return new BitrixManagedConnection(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ManagedConnection matchManagedConnections(
            Set connectionSet,
            Subject subject,
            ConnectionRequestInfo connectionRequestInfo
    ) throws ResourceException {
        log.debug("Matching managed connections");
        return null; // Let container handle pooling
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        this.logWriter = printWriter;
    }

    public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getLogger(BitrixManagedConnectionFactory.class.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BitrixManagedConnectionFactory that)) return false;
        return Objects.equals(webhookUrl, that.webhookUrl) &&
               Objects.equals(documentTemplateId, that.documentTemplateId) &&
               Objects.equals(uploadToCrmFallback, that.uploadToCrmFallback) &&
               Objects.equals(dealMethod, that.dealMethod) &&
               Objects.equals(assignedById, that.assignedById) &&
               Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                webhookUrl, documentTemplateId, uploadToCrmFallback,
                dealMethod, assignedById, categoryId
        );
    }
}
