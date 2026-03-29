# Spring Security + JAAS Implementation Guide

## Overview

This document describes the implementation of role-based access control (RBAC) for the BLSS application using Spring Security integrated with JAAS (Java Authentication and Authorization Service). User accounts are stored in an XML file, and HTTP Basic authentication is used for API security.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      HTTP Request                                │
│                   (with Basic Auth)                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SecurityFilterChain                             │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐    │
│  │ HTTP Basic  │→ │  DaoAuth     │→ │  CustomUserDetails  │    │
│  │  Auth       │  │  Provider    │  │  Service            │    │
│  └─────────────┘  └──────────────┘  └──────────┬──────────┘    │
│                                                 │               │
│                                                 ▼               │
│                                      ┌─────────────────┐        │
│                                      │ XmlUserRepository│       │
│                                      │  (XML Parser)    │        │
│                                      └─────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   @PreAuthorize Checks                           │
│              (Method-level Security)                             │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### 1. XML User Store (`src/main/resources/security/users.xml`)

User accounts are stored in XML format with the following structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<users>
    <user>
        <username>admin</username>
        <password>$2a$10$...</password>  <!-- BCrypt hash -->
        <enabled>true</enabled>
        <roles>
            <role>ADMIN</role>
        </roles>
    </user>
</users>
```

### 2. XmlUserRepository

Spring repository that loads and caches users from the XML file:
- Parses XML using Jackson XML
- Caches users in memory for performance
- Provides lookup by username

### 3. XmlLoginModule (JAAS)

JAAS LoginModule implementation for XML-based authentication:
- Implements `javax.security.auth.spi.LoginModule`
- Uses BCrypt for password verification
- Creates Principal objects for username and roles
- Supports JAAS integration patterns

### 4. CustomUserDetailsService

Spring Security UserDetailsService implementation:
- Bridges XML user store with Spring Security
- Converts XML roles to Spring GrantedAuthority
- Handles user enabled/disabled status

### 5. SecurityConfig

Main Spring Security configuration:
- HTTP Basic authentication
- Role-based URL security
- Method-level security with @PreAuthorize
- Custom authentication/authorization error handlers

## User Roles

| Role | Description | Access Level |
|------|-------------|--------------|
| `ADMIN` | System administrator | Full access to all endpoints |
| `MANAGER` | Business manager | Orders, inventory, delivery points |
| `CONSULTANT` | Customer consultant | Order creation, status updates, customer operations |
| `WAREHOUSE` | Warehouse worker | Inventory management, delivery operations |

## Endpoint Security Matrix

| Endpoint | Method | ADMIN | MANAGER | CONSULTANT | WAREHOUSE |
|----------|--------|-------|---------|------------|-----------|
| `/order/**` | POST, GET, PATCH | ✅ | ✅ | ✅ | ❌ |
| `/inventory/**` | POST, PUT, PATCH | ✅ | ✅ | ❌ | ✅ |
| `/inventory/**` | GET | ✅ | ✅ | ✅ | ✅ |
| `/mark-delivered` | POST | ✅ | ✅ | ✅ | ✅ |
| `/users/**` | POST | ✅ | ❌ | ❌ | ❌ |
| `/delivery-points/**` | POST | ✅ | ✅ | ❌ | ❌ |

## Default Users

| Username | Password | Roles |
|----------|----------|-------|
| `admin` | `admin123` | ADMIN |
| `manager` | `manager123` | MANAGER |
| `consultant` | `consultant123` | CONSULTANT |
| `warehouse` | `warehouse123` | WAREHOUSE |
| `senior_consultant` | `senior123` | CONSULTANT, WAREHOUSE |

> ⚠️ **Important**: Change default passwords in production!

## Usage Examples

### cURL Examples

```bash
# Create an order (as consultant)
curl -X POST http://localhost:21001/order/create \
  -u consultant:consultant123 \
  -H "Content-Type: application/json" \
  -d '{"owner": "user-id", "location": "loc-id", "productIds": ["prod-id"]}'

# Get inventory (as warehouse)
curl -X GET http://localhost:21001/inventory/products \
  -u warehouse:warehouse123

# Create a product (as warehouse)
curl -X POST http://localhost:21001/inventory/products \
  -u warehouse:warehouse123 \
  -H "Content-Type: application/json" \
  -d '{"name": "Product", "price": 100.0, "initialCount": 50}'

# Create a user (admin only)
curl -X POST http://localhost:21001/users \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"email": "new@example.com"}'
```

### Error Responses

**401 Unauthorized** (Authentication required):
```json
{
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

**403 Forbidden** (Insufficient privileges):
```json
{
  "error": "Forbidden",
  "message": "Insufficient privileges"
}
```

## Adding New Users

1. Generate BCrypt password hash:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("yourPassword");
```

2. Add user to `src/main/resources/security/users.xml`:
```xml
<user>
    <username>newuser</username>
    <password>$2a$10$...</password>
    <enabled>true</enabled>
    <roles>
        <role>CONSULTANT</role>
    </roles>
</user>
```

3. Restart the application (users are loaded at startup)

## Configuration

### application.yaml

No additional configuration required. Security is auto-configured.

### Customizing Security

To modify security settings, edit `SecurityConfig.java`:

```java
// Change realm name
.httpBasic(basic -> basic.realmName("Custom Realm"))

// Add public endpoints
.requestMatchers("/public/**").permitAll()

// Modify role requirements
.requestMatchers("/admin/**").hasRole("ADMIN")
```

## Testing

### Unit Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired MockMvc mockMvc;

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/order/123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthorizedAccess() throws Exception {
        mockMvc.perform(get("/order/123")
                .with(httpBasic("consultant", "consultant123")))
            .andExpect(status().isOk());
    }

    @Test
    void testForbiddenAccess() throws Exception {
        mockMvc.perform(post("/users")
                .with(httpBasic("consultant", "consultant123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

## JAAS Integration

The `XmlLoginModule` can be used with standard JAAS configuration:

```java
LoginContext loginContext = new LoginContext("XmlLogin", 
    new CallbackHandler() {
        public void handle(Callback[] callbacks) {
            // Handle NameCallback and PasswordCallback
        }
    });
loginContext.login();
Subject subject = loginContext.getSubject();
```

## Security Considerations

1. **Password Storage**: All passwords are hashed using BCrypt
2. **Stateless Authentication**: No session storage, each request is authenticated
3. **HTTPS**: Always use HTTPS in production
4. **Password Policy**: Implement password complexity requirements
5. **Account Lockout**: Consider implementing account lockout after failed attempts
6. **Audit Logging**: Log authentication attempts for security auditing

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: Check username/password, ensure user exists in XML
2. **403 Forbidden**: User authenticated but lacks required role
3. **Users not loading**: Check XML file path and format
4. **BCrypt mismatch**: Ensure passwords are properly hashed

### Debug Logging

Enable security debug logging in `application.yaml`:
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.blss.blss.security: DEBUG
```
