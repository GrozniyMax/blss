package com.blss.blss.security.jaas;

import org.springframework.security.authentication.jaas.AuthorityGranter;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

public class RolePrincipalAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {
        if (principal instanceof XmlLoginModule.RolePrincipal rolePrincipal) {
            String role = rolePrincipal.getName();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
            return Collections.singleton(role);
        }
        return null;
    }
}
