package com.blss.orderservice.bitrix;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки Bitrix24 JCA коннектора.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bitrix")
public class BitrixProperties {

    private boolean enabled = false;

    private String webhookUrl;

    private Integer documentTemplateId = 0;

    private Boolean uploadToCrmFallback = true;

    private String dealMethod = "crm.deal.add.json";

    private Long assignedById = 0L;

    private Integer categoryId = 0;

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

    private String defaultProductMeasureName = "шт";
    private String defaultTaxTitle = "Без НДС";
    private String defaultTaxRate = "Без НДС";
    private String defaultClientPhone = "";

}
