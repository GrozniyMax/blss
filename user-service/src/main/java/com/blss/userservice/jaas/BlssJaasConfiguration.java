package com.blss.userservice.jaas;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

public class BlssJaasConfiguration extends Configuration {

    public static final String LOGIN_CONTEXT_NAME = "BLSS";

    private final AppConfigurationEntry[] appConfigurationEntry;

    public BlssJaasConfiguration(String xmlPath) {
        Map<String, String> options = new HashMap<>();
        if (xmlPath != null && !xmlPath.isBlank()) {
            options.put("xmlPath", xmlPath);
        }
        this.appConfigurationEntry = new AppConfigurationEntry[] {
                new AppConfigurationEntry(
                        XmlLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options
                )
        };
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!LOGIN_CONTEXT_NAME.equals(name)) {
            return null;
        }
        return appConfigurationEntry;
    }
}
