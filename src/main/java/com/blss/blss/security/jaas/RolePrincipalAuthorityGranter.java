package com.blss.blss.security.jaas;

import com.blss.blss.security.Role;
import org.springframework.security.authentication.jaas.AuthorityGranter;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
 * Преобразует Principal роли в Spring Security Authority.
 * Добавляет префикс "ROLE_" к имени роли для совместимости с Spring Security.
 */
public class RolePrincipalAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {
        if (principal instanceof XmlLoginModule.RolePrincipal rolePrincipal) {
            String roleName = rolePrincipal.getName();
            // Преобразуем имя роли в формат Spring Security (ROLE_XXX)
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            return Collections.singleton(roleName);
        }
        return null;
    }
}
