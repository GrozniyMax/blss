package com.blss.orderservice.bitrix;

import com.blss.bitrixjca.api.BitrixConnection;
import com.blss.bitrixjca.api.BitrixConnectionFactory;
import com.blss.bitrixjca.impl.BitrixManagedConnectionFactory;
import com.blss.bitrixjca.impl.BitrixResourceAdapter;
import jakarta.resource.ResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Bitrix24 JCA connector.
 * Creates and configures the JCA connection factory for Bitrix24 REST API integration.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BitrixProperties.class)
public class BitrixJcaConfiguration {

    private final BitrixProperties properties;

    /**
     * Creates the Bitrix24 resource adapter.
     */
    @Bean
    public BitrixResourceAdapter bitrixResourceAdapter() {
        log.info("Creating Bitrix24 Resource Adapter");
        return new BitrixResourceAdapter();
    }

    /**
     * Creates the managed connection factory for Bitrix24.
     * Configured with properties from application.yaml.
     */
    @Bean
    @ConditionalOnProperty(prefix = "bitrix", name = "webhook-url")
    public BitrixManagedConnectionFactory bitrixManagedConnectionFactory() {
        log.info("Creating Bitrix24 Managed Connection Factory");
        BitrixManagedConnectionFactory mcf = new BitrixManagedConnectionFactory();
        mcf.setWebhookUrl(properties.getWebhookUrl());
        mcf.setDocumentTemplateId(properties.getDocumentTemplateId());
        mcf.setUploadToCrmFallback(properties.getUploadToCrmFallback());
        mcf.setDealMethod(properties.getDealMethod());
        mcf.setAssignedById(properties.getAssignedById());
        mcf.setCategoryId(properties.getCategoryId());
        mcf.setMyCompanyName(properties.getMyCompanyName());
        mcf.setMyCompanyInn(properties.getMyCompanyInn());
        mcf.setMyCompanyKpp(properties.getMyCompanyKpp());
        mcf.setMyCompanyAddress(properties.getMyCompanyAddress());
        mcf.setMyCompanyPhone(properties.getMyCompanyPhone());
        mcf.setMyCompanyBankName(properties.getMyCompanyBankName());
        mcf.setMyCompanyBik(properties.getMyCompanyBik());
        mcf.setMyCompanyAccNum(properties.getMyCompanyAccNum());
        mcf.setMyCompanyCorAccNum(properties.getMyCompanyCorAccNum());
        mcf.setMyCompanyDirector(properties.getMyCompanyDirector());
        mcf.setDefaultProductMeasureName(properties.getDefaultProductMeasureName());
        mcf.setDefaultTaxTitle(properties.getDefaultTaxTitle());
        mcf.setDefaultTaxRate(properties.getDefaultTaxRate());
        mcf.setDefaultTaxValue(properties.getDefaultTaxValue());
        mcf.setDefaultClientPhone(properties.getDefaultClientPhone());
        mcf.setDefaultClientInn(properties.getDefaultClientInn());
        mcf.setDefaultClientKpp(properties.getDefaultClientKpp());
        mcf.setDefaultClientBankName(properties.getDefaultClientBankName());
        mcf.setDefaultClientBik(properties.getDefaultClientBik());
        mcf.setDefaultClientAccNum(properties.getDefaultClientAccNum());
        mcf.setDefaultClientCorAccNum(properties.getDefaultClientCorAccNum());
        return mcf;
    }

    /**
     * Creates the connection factory for Bitrix24.
     * This is the main bean that application code uses to obtain connections.
     */
    @Bean
    @ConditionalOnProperty(prefix = "bitrix", name = "webhook-url")
    public BitrixConnectionFactory bitrixConnectionFactory(
            BitrixManagedConnectionFactory managedConnectionFactory
    ) throws ResourceException {
        log.info("Creating Bitrix24 Connection Factory");
        return (BitrixConnectionFactory) managedConnectionFactory.createConnectionFactory();
    }

    /**
     * Creates a connection to Bitrix24.
     * This is a prototype-scoped bean that provides a connection handle.
     */
    @Bean
    @ConditionalOnProperty(prefix = "bitrix", name = {"webhook-url", "enabled"}, havingValue = "true")
    public BitrixConnection bitrixConnection(
            BitrixConnectionFactory connectionFactory
    ) throws ResourceException {
        log.info("Creating Bitrix24 Connection");
        return connectionFactory.getConnection();
    }
}
