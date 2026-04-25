package com.blss.userservice.xml;

import com.blss.userservice.security.Role;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user account loaded from XML configuration.
 */
@Data
@JacksonXmlRootElement(localName = "users")
public class XmlUser {

    @JacksonXmlProperty(localName = "user")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<UserAccount> users;

    @Data
    public static class UserAccount {

        @JacksonXmlProperty(localName = "username")
        private String username;

        @JacksonXmlProperty(localName = "password")
        private String password;

        @JacksonXmlProperty(localName = "enabled")
        private boolean enabled = true;

        @JacksonXmlProperty(localName = "roles")
        private Roles roles;
    }

    @Data
    public static class Roles {

        @JacksonXmlProperty(localName = "role")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Role> role = new ArrayList<>();
    }
}
