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

## Quick Start

```bash
git clone https://github.com/jzavalaq/multitenant-ecommerce-api.git
cd multitenant-ecommerce-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Examples

### Authentication

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{"username":"user1","email":"user@example.com","password":"Secure123!","role":"CUSTOMER"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{"username":"user1","password":"Secure123!"}'
```

### Products

```bash
TOKEN="your-jwt-token"

# Create product (vendor)
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{"name":"Wireless Headphones","price":99.99,"categoryId":1,"stockQuantity":100}'

# Get products
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant-001"
```

### Shopping Cart

```bash
# Add to cart
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{"productId":1,"variantId":1,"quantity":2}'

# Get cart
curl -X GET http://localhost:8080/api/v1/cart \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant-001"
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_URL` | PostgreSQL connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing key |

## License

MIT License - see [LICENSE](LICENSE)
