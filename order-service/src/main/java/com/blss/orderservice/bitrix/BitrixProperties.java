package com.blss.orderservice.bitrix;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuration properties for Bitrix24 JCA connector.
 * These properties are used to configure the JCA connection factory.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bitrix")
public class BitrixProperties {

    /**
     * Enable Bitrix24 integration (default: false)
     */
    private boolean enabled = false;

    /**
     * Bitrix24 webhook URL for REST API access
     */
    private String webhookUrl;

    /**
     * Document template ID for document generator (0 if not used)
     */
    private Integer documentTemplateId = 0;

    /**
     * Enable CRM deal creation as fallback (default: true)
     */
    private Boolean uploadToCrmFallback = true;

    /**
     * CRM deal method name (default: crm.deal.add.json)
     */
    private String dealMethod = "crm.deal.add.json";

    /**
     * User ID to assign deals to (0 if not used)
     */
    private Long assignedById = 0L;

    /**
     * Category ID for deals (0 if not used)
     */
    private Integer categoryId = 0;

    /**
     * Default company details used by document templates.
     */
    private String myCompanyName = "";
    private String myCompanyInn = "";
    private String myCompanyKpp = "";
    private String myCompanyAddress = "";
    private String myCompanyPhone = "";
    private String myCompanyBankName = "";
    private String myCompanyBik = "";
    private String myCompanyAccNum = "";
    private String myCompanyCorAccNum = "";
    private String myCompanyDirector = "";

    /**
     * Defaults for generated act line items.
     */
    private String defaultProductMeasureName = "шт";
    private String defaultTaxTitle = "Без НДС";
    private String defaultTaxRate = "Без НДС";
    private BigDecimal defaultTaxValue = BigDecimal.ZERO;
    private String defaultClientPhone = "";
    private String defaultClientInn = "";
    private String defaultClientKpp = "";
    private String defaultClientBankName = "";
    private String defaultClientBik = "";
    private String defaultClientAccNum = "";
    private String defaultClientCorAccNum = "";

}
