# Multi-Tenant E-Commerce Platform

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](Dockerfile)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql)](https://www.postgresql.org/)

A robust multi-tenant e-commerce platform backend built with Java 21 and Spring Boot 3.2. This platform supports multiple vendors (tenants) with isolated product catalogs, JWT-based authentication with role-based access control, and a complete shopping cart system.

## Features

- **Multi-Tenancy**: Complete vendor data isolation with tenant context
- **Product Management**: Variants, categories, inventory tracking
- **Shopping Cart**: Real-time stock validation, bulk operations
- **Role-Based Access**: Admin, Vendor, Customer roles

## Tech Stack

| Technology | Version | Description |
|------------|---------|-------------|
| Java | 21 | Runtime environment |
| Spring Boot | 3.2.5 | Application framework |
| Spring Data JPA | 3.2.5 | Data access |
| Spring Security | 6.x | Authentication & authorization |
| PostgreSQL | 15+ | Production database |
| H2 | 2.x | Development database |
| JWT (jjwt) | 0.12.5 | Token-based authentication |
| Flyway | 10.x | Database migrations |
| Lombok | 1.18.x | Boilerplate reduction |
| SpringDoc OpenAPI | 2.5.0 | API documentation |

## Prerequisites

- Java 21 or later
- Maven 3.9+
- PostgreSQL 15+ (for production)
- Docker (optional, for containerized deployment)

## Build Instructions

```bash
# Clone the repository
git clone https://github.com/jzavalaq/multitenant-ecommerce-api.git
cd multitenant-ecommerce-api

# Build the project
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Run tests only
mvn test
```

## Run Instructions

### Development (H2 Database)

```bash
# Run with dev profile (uses in-memory H2 database)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or with java -jar
java -jar target/platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Production (PostgreSQL)

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/ecommerce
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long

# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Or with java -jar
java -jar target/platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Docker Run Instructions

### Quick Start with Docker Compose

The easiest way to run the entire stack (application + PostgreSQL database) is using Docker Compose:

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your configuration (optional for development)
# nano .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop services and remove volumes
docker-compose down -v
```

The application will be available at:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

### Manual Docker Run

```bash
# Build the Docker image
docker build -t multitenant-ecommerce-api .

# Run the container
docker run -d \
  --name ecommerce-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ecommerce \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long \
  multitenant-ecommerce-api

# View logs
docker logs -f ecommerce-api
```

## API Documentation

Once the application is running, access the Swagger UI at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## API Examples

### Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "tenantSlug": "tenant-001",
    "role": "CUSTOMER"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "tenantSlug": "tenant-001"
  }'
```

### Products

```bash
TOKEN="your-jwt-token"

# Get products with pagination
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Get products by category
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=20&categoryId=1" \
  -H "Authorization: Bearer $TOKEN"

# Get a specific product
curl -X GET "http://localhost:8080/api/v1/products/1" \
  -H "Authorization: Bearer $TOKEN"

# Create a product (requires VENDOR or ADMIN role)
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Headphones",
    "description": "High-quality wireless headphones with noise cancellation",
    "categoryId": 1,
    "variants": [
      {
        "sku": "WH-BLK-001",
        "price": 99.99,
        "stock": 100
      }
    ]
  }'

# Update a product (requires VENDOR or ADMIN role)
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Headphones Pro",
    "description": "Premium wireless headphones",
    "categoryId": 1,
    "variants": [
      {
        "sku": "WH-BLK-001",
        "price": 129.99,
        "stock": 50
      }
    ]
  }'

# Delete a product (requires VENDOR or ADMIN role)
curl -X DELETE http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Categories

```bash
# Get categories with pagination
curl -X GET "http://localhost:8080/api/v1/categories?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Get a specific category
curl -X GET http://localhost:8080/api/v1/categories/1" \
  -H "Authorization: Bearer $TOKEN"

# Create a category (requires VENDOR or ADMIN role)
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Electronics"
  }'

# Create a subcategory
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Headphones",
    "parentId": 1
  }'
```

### Shopping Cart

```bash
# Get cart (requires CUSTOMER role)
curl -X GET http://localhost:8080/api/v1/cart \
  -H "Authorization: Bearer $TOKEN"

# Add item to cart
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "variantId": 1,
    "quantity": 2
  }'

# Update cart item quantity
curl -X PUT "http://localhost:8080/api/v1/cart/items/1?quantity=3" \
  -H "Authorization: Bearer $TOKEN"

# Remove item from cart
curl -X DELETE http://localhost:8080/api/v1/cart/items/1 \
  -H "Authorization: Bearer $TOKEN"

# Clear cart
curl -X DELETE http://localhost:8080/api/v1/cart \
  -H "Authorization: Bearer $TOKEN"
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (dev/prod) | dev |
| `SERVER_PORT` | Server port | 8080 |
| `DB_URL` | PostgreSQL connection URL | - |
| `DB_USERNAME` | Database username | - |
| `DB_PASSWORD` | Database password | - |
| `JWT_SECRET` | JWT signing key (256-bit minimum) | change-me-in-production... |
| `ALLOWED_ORIGINS` | CORS allowed origins | http://localhost:3000 |
| `H2_CONSOLE_ENABLED` | Enable H2 console (dev only) | false |
| `HIKARI_MAX_POOL_SIZE` | HikariCP max pool size | 20 |
| `HIKARI_MIN_IDLE` | HikariCP min idle connections | 5 |

## License

MIT License - see [LICENSE](LICENSE)
