package com.blss.orderservice.bitrix;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bitrix")
public class BitrixProperties {

    private boolean enabled;
    private String webhookUrl;
    private String dealMethod = "crm.deal.add.json";
    private Long assignedById = 0L;
    private Integer categoryId = 0;
    private Integer documentTemplateId = 0;
    private boolean uploadToCrmFallback = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getDealMethod() {
        return dealMethod;
    }

    public void setDealMethod(String dealMethod) {
        this.dealMethod = dealMethod;
    }

    public Long getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(Long assignedById) {
        this.assignedById = assignedById;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getDocumentTemplateId() {
        return documentTemplateId;
    }

    public void setDocumentTemplateId(Integer documentTemplateId) {
        this.documentTemplateId = documentTemplateId;
    }

    public boolean isUploadToCrmFallback() {
        return uploadToCrmFallback;
    }

    public void setUploadToCrmFallback(boolean uploadToCrmFallback) {
        this.uploadToCrmFallback = uploadToCrmFallback;
    }
}
