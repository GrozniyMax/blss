package com.blss.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.users")
public class SecurityUsersProperties {

    /**
     * Path to the XML file containing user accounts.
     */
    private String xmlPath = "./users.xml";
}
