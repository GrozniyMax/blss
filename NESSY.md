# BLSS Project Context

## Project Overview

**BLSS** is a Spring Boot-based warehouse and order pickup management system. It provides a REST API for managing inventory, orders, delivery points, and users with role-based access control integrated with Camunda BPM for workflow automation.

### Core Purpose
The system automates the business process of order pickup at delivery points (ПВЗ - пункты выдачи заказов), handling the workflow from order creation through inventory reservation, preparation, pickup, and returns.

### Technology Stack
- **Runtime**: Java 17
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: PostgreSQL 15 (via Liquibase migrations)
- **Security**: Spring Security + JAAS with XML-based user store
- **BPM Engine**: Camunda 7.20.0 (standalone, external task client)
- **Containerization**: Docker Compose (PostgreSQL + pgAdmin + Camunda)

### Architecture
```
com.blss.blss/
├── controller/     # REST API endpoints (Inventory, Order, PVZ, User, DeliveryPoint)
├── service/        # Business logic layer (including Camunda workers)
├── domain/         # Domain entities (Order, Product, User, DeliveryPoint, StoreItem)
├── db/             # Repository interfaces and database access
├── dto/            # Data Transfer Objects (input/output)
├── security/       # Spring Security + JAAS configuration
├── config/         # Application configuration
├── camunda/        # Camunda process client and external task workers
└── exception/      # Custom exception handling
```

## Building and Running

### Prerequisites
- JDK 17+
- Docker and Docker Compose (for database and Camunda)
- Environment variables set (see `.env`)

### Quick Start

1. **Start the infrastructure (PostgreSQL + pgAdmin + Camunda):**
   ```bash
   docker-compose up -d
   ```

2. **Build the application:**
   ```bash
   ./gradlew clean build
   ```
   
   > Note: Requires environment variables `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` to be set during build (resource filtering).

3. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```
   
   Or run the built JAR:
   ```bash
   java -jar build/libs/blss-0.0.1-SNAPSHOT.jar
   ```

### Configuration

**Environment Variables** (required for build):
| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `studs` |
| `POSTGRES_USER` | Database user | `s408145` |
| `POSTGRES_PASSWORD` | Database password | `JLzD%6772` |
| `SPRING_PORT` | Application port | `25102` |

**Application Settings** (`src/main/resources/application.yaml`):
- Server port: `${SPRING_PORT:25102}`
- Database: `jdbc:postgresql://localhost:5432/${POSTGRES_DB:studs}`
- Camunda REST: `http://localhost:8080/engine-rest`
- Users XML path: `${SECURITY_USERS_XML_PATH:./users.xml}`

### Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## API Endpoints

### Inventory Management
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/inventory/products` | POST | ADMIN, MANAGER | Create product |
| `/inventory/products/{id}` | PUT | ADMIN, MANAGER | Update product |
| `/inventory/products/{id}/count` | PATCH | ADMIN, MANAGER | Change stock count |
| `/inventory/products/{id}` | GET | All | Get product details |
| `/inventory/products` | GET | All | List all products |

### Order Management
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/order/create` | POST | USER, ADMIN | Create order (with security check) |
| `/order/{id}` | GET | All | Get order details (with access check) |
| `/order/{id}/status/next` | PATCH | ADMIN, MANAGER, CONSULTANT | Advance order status |
| `/order/{id}/status/cancel` | PATCH | ADMIN, MANAGER, CONSULTANT | Cancel order |

### Delivery Points (ПВЗ)
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/mark-delivered` | POST | ADMIN, WAREHOUSE | Mark order item as delivered |

### User Management
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/users` | POST | ADMIN | Create user |

### Delivery Point Registry
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/delivery-points` | POST | ADMIN, MANAGER | Create delivery point |

### Camunda Processes
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/camunda/processes/products` | POST | Start product creation process |
| `/camunda/processes/orders` | POST | Start order creation process |
| `/camunda/processes/pickup` | POST | Start order pickup process |

## Security

### Authentication
- HTTP Basic Authentication
- Users stored in XML file (`users.xml`)
- Passwords stored in plain text (for development)

### User Roles
| Role | Description |
|------|-------------|
| `ADMIN` | Full system access |
| `MANAGER` | Orders, inventory, delivery points |
| `CONSULTANT` | Order creation, status updates, customer operations |
| `WAREHOUSE` | Inventory management, product deliveries |

### Default Users (Development)
| Username | Password | Roles |
|----------|----------|-------|
| `admin` | `admin123` | ADMIN |
| `manager` | `manager123` | MANAGER |
| `consultant` | `consultant123` | CONSULTANT |
| `warehouse` | `warehouse123` | WAREHOUSE |
| `senior_consultant` | `senior123` | CONSULTANT, WAREHOUSE |

> ⚠️ **Security Warning**: Change default passwords in production!

## Domain Model

### Core Entities
- **User** - Customer/client with email
- **DeliveryPoint** - Pickup location (ПВЗ) with name and address
- **Product** - Catalog item with name and price
- **Order** - Customer order with status, location, total amount, dates
- **OrderItem** - Order line item linking order to product
- **StoreItem** - Warehouse inventory (product + count)

### Order Status Flow
```
CREATED → PROCESSING → IN_DELIVERY → READY_FOR_PICKUP → DONE
                                         ↓
                                    CANCELED
```

## Database Schema

Managed by Liquibase (`src/main/resources/db/changelog/`):
- `users` - User accounts
- `delivery_points` - Pickup locations
- `products` - Product catalog
- `store` - Inventory counts
- `orders` - Customer orders
- `order_items` - Order line items

See `changelog-master.xml` for migration structure.

## Camunda BPM Integration

### Processes
- `warehouse-product-create` - Product creation workflow
- `warehouse-order-create` - Order creation with validation and reservation
- `warehouse-order-pickup` - Order pickup with verification and completion

### External Task Topics
- `product.create` - Calls `StoreService.createProduct(...)`
- `order.create` - Calls `OrderService.createOrder(...)`
- `order.pickup.verify` - Verifies order is in `READY_FOR_PICKUP` status
- `order.pickup.complete` - Transitions order to `DONE`
- `order.pickup.reject` - Transitions order to `CANCELED`

### Configuration
- Camunda REST URL: `http://localhost:8080/engine-rest`
- Worker ID: `blss-worker`
- Basic Auth: `admin/admin123`

## Development Conventions

### Code Style
- Lombok for boilerplate reduction (`@RequiredArgsConstructor`, `@FieldDefaults`)
- Record classes for domain entities (immutable data carriers)
- Constructor injection (field injection with `makeFinal = true`)
- Package-private visibility by default

### Testing Practices
- HTTP files in `src/main/test/` for manual API testing
- Postman collection available in `src/main/test/postman/`
- Integration tests with Testcontainers (PostgreSQL)

### HTTP Files
HTTP request files available in `src/main/test/` for manual API testing:
- `inventory.http`
- `order.http`
- `users.http`
- `delivery_points.http`
- `pvz.http`

## CI/CD

### GitHub Actions (`.github/workflows/deploy.yml`)
- Triggered on PR to `main` branch
- Builds with Gradle (JDK 17)
- Deploys JAR via SCP to remote server
- Requires secrets: `POSTGRES_*`, `SERVER_*`, `SPRING_PORT`

## Documentation

| File | Description |
|------|-------------|
| `docs/Доменная модель.md` | Domain model with PlantUML diagrams |
| `docs/lab1.yaml` | OpenAPI 3.0 specification |
| `docs/SECURITY.md` | Security implementation guide |
| `docs/ROLES.md` | Role-based access control documentation |
| `docs/camunda-mapping.md` | Camunda BPM process mapping |
| `docs/отчет.md` | Project report with UML diagrams and API specs |
| `docs/diagram (1).bpmn` | BPMN business process model |

## Project Structure

```
blss/
├── src/
│   ├── main/
│   │   ├── java/com/blss/blss/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic (including Camunda workers)
│   │   │   ├── domain/          # Entities
│   │   │   ├── db/              # Repositories
│   │   │   ├── dto/             # DTOs
│   │   │   ├── security/        # Security config
│   │   │   ├── camunda/         # Camunda client and workers
│   │   │   └── config/          # Spring config
│   │   ├── resources/
│   │   │   ├── application.yaml
│   │   │   ├── db/changelog/    # Liquibase migrations
│   │   │   ├── bpmn/            # Camunda BPMN processes
│   │   │   ├── forms/           # Camunda forms
│   │   │   └── security/        # XML user store
│   │   └── test/                # Tests
│   └── main/test/               # HTTP files for manual testing
├── docs/                        # Documentation
├── docker-compose.yml           # Infrastructure containers
├── build.gradle.kts             # Gradle build config
├── users.xml                    # User accounts (dev)
└── .env                         # Environment variables
```

## Common Commands

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Test
./gradlew test

# Infrastructure
docker-compose up -d
docker-compose down

# Check dependencies
./gradlew dependencies
```

## External Resources

- **GitHub Repository**: https://github.com/GrozniyMax/blss
- **PostgreSQL**: localhost:5432
- **pgAdmin**: http://localhost:6980 (v.amuz@gmail.com / 1488)
- **Camunda**: http://localhost:8080 (admin / admin123)
- **Application**: http://localhost:${SPRING_PORT:25102}
