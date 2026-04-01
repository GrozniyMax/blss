package com.blss.blss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security.users")
public class SecurityUsersProperties {

    /**
     * Path to the XML file containing user accounts.
     */
    private String xmlPath = "./";
}
