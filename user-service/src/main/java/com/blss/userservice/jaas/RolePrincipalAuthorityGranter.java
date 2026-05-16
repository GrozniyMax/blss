package com.blss.userservice.jaas;

import org.springframework.security.authentication.jaas.AuthorityGranter;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

public class RolePrincipalAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {
        if (principal instanceof XmlLoginModule.RolePrincipal rolePrincipal) {
            String roleName = rolePrincipal.getName();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            return Collections.singleton(roleName);
        }
        return null;
    }
}
