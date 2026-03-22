package com.blss.blss.security.jaas;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlssJaasConfiguration extends Configuration {

    public static final String LOGIN_CONTEXT_NAME = "BLSS";

    private static final AppConfigurationEntry[] APP_CONFIGURATION_ENTRY;

    static {
        APP_CONFIGURATION_ENTRY = new AppConfigurationEntry[] {
            new AppConfigurationEntry(
                XmlLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    Map.<String, String>of()
            )
        };
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!LOGIN_CONTEXT_NAME.equals(name)) {
            return null;
        }
        return APP_CONFIGURATION_ENTRY;
    }
}
