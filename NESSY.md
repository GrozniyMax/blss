# BLSS Project Context

## Project Overview

**BLSS** is a Spring Boot-based warehouse and order pickup management system. It provides a REST API for managing inventory, orders, delivery points, and users with role-based access control.

### Core Purpose
The system automates the business process of order pickup at delivery points (ПВЗ - пункты выдачи заказов), handling the workflow from order creation through inventory reservation, preparation, pickup, and returns.

### Technology Stack
- **Runtime**: Java 17
- **Framework**: Spring Boot 4.0.2
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: PostgreSQL 15 (via Liquibase migrations)
- **Security**: Spring Security + JAAS with XML-based user store
- **Containerization**: Docker Compose (PostgreSQL + pgAdmin)

### Architecture
```
com.blss.blss/
├── controller/     # REST API endpoints (Inventory, Order, PVZ, User, DeliveryPoint)
├── service/        # Business logic layer
├── domain/         # Domain entities (Order, Product, User, DeliveryPoint, StoreItem)
├── db/             # Repository interfaces and database access
├── dto/            # Data Transfer Objects (input/output)
├── security/       # Spring Security + JAAS configuration
├── config/         # Application configuration
└── exception/      # Custom exception handling
```

## Building and Running

### Prerequisites
- JDK 17+
- Docker and Docker Compose (for database)
- Environment variables set (see `.env`)

### Quick Start

1. **Start the database:**
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
| `SPRING_PORT` | Application port | `3000` |

**Application Settings** (`src/main/resources/application.yaml`):
- Server port: `21001`
- Database: `jdbc:postgresql://localhost:5432/studs`
- Users XML path: `/Users/m.s.taranenko/IdeaProjects/blss/users.xml`

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
| `/inventory/products` | POST | ADMIN, MANAGER, WAREHOUSE | Create product |
| `/inventory/products/{id}` | PUT | ADMIN, MANAGER, WAREHOUSE | Update product |
| `/inventory/products/{id}/count` | PATCH | ADMIN, MANAGER, WAREHOUSE | Change stock count |
| `/inventory/products/{id}` | GET | All | Get product details |
| `/inventory/products` | GET | All | List all products |

### Order Management
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/order/create` | POST | USER, CONSULTANT, MANAGER, ADMIN | Create order |
| `/order/{id}` | GET | USER, CONSULTANT, MANAGER, ADMIN | Get order details |
| `/order/{id}/status/next` | PATCH | CONSULTANT, MANAGER, ADMIN | Advance order status |
| `/order/{id}/status/cancel` | PATCH | CONSULTANT, MANAGER, ADMIN | Cancel order |

### Delivery Points (ПВЗ)
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/mark-delivered` | POST | All | Mark order item as delivered |

### User Management
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/users` | POST | ADMIN | Create user |

### Delivery Point Registry
| Endpoint | Method | Roles | Description |
|----------|--------|-------|-------------|
| `/delivery-points` | POST | ADMIN, MANAGER | Create delivery point |

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

## Development Conventions

### Code Style
- Lombok for boilerplate reduction (`@RequiredArgsConstructor`, `@FieldDefaults`)
- Record classes for DTOs (immutable data carriers)
- Constructor injection (field injection with `makeFinal = true`)
- Package-private visibility by default

### Testing Practices
- Testcontainers for integration tests (PostgreSQL)
- Spring Boot Test slices for unit tests
- Postman collection available in `src/main/test/postman/`

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
| `docs/отчет.md` | Project report with UML diagrams and API specs |
| `docs/diagram (1).bpmn` | BPMN business process model |

## Project Structure

```
blss/
├── src/
│   ├── main/
│   │   ├── java/com/blss/blss/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── domain/          # Entities
│   │   │   ├── db/              # Repositories
│   │   │   ├── dto/             # DTOs
│   │   │   ├── security/        # Security config
│   │   │   └── config/          # Spring config
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── db/changelog/    # Liquibase migrations
│   │       └── security/        # XML user store
│   └── test/                    # Tests
├── docs/                        # Documentation
├── docker-compose.yml           # Database containers
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

# Database
docker-compose up -d
docker-compose down

# Check dependencies
./gradlew dependencies

# Generate OpenAPI docs (if configured)
./gradlew openapiGenerate
```

## External Resources

- **GitHub Repository**: https://github.com/GrozniyMax/blss
- **PostgreSQL**: localhost:5432
- **pgAdmin**: http://localhost:6980 (v.amuz@gmail.com / 1488)
- **Application**: http://localhost:21001
