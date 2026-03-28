# BUILD_CONTEXT — Multi-Tenant E-Commerce Platform
Generated: 2026-03-26

## Quick Reference
- Language: Java
- Framework: Spring Boot 3.2.5
- Database: H2 (dev) / PostgreSQL (prod)
- Auth: JWT
- Tests: 75 passing
- Coverage: 46%

## Entities
Tenant, User, Category, Product, ProductVariant, Cart, CartItem

## Endpoints
POST:/api/v1/auth/register, POST:/api/v1/auth/login,
GET:/api/v1/products, POST:/api/v1/products, GET:/api/v1/products/{id}, PUT:/api/v1/products/{id}, DELETE:/api/v1/products/{id},
GET:/api/v1/categories, POST:/api/v1/categories, GET:/api/v1/categories/{id}, PUT:/api/v1/categories/{id}, DELETE:/api/v1/categories/{id},
GET:/api/v1/cart, POST:/api/v1/cart/items, PUT:/api/v1/cart/items/{itemId}, DELETE:/api/v1/cart/items/{itemId}, DELETE:/api/v1/cart

## Key Files
- Main: src/main/java/com/ecommerce/platform/PlatformApplication.java
- Config: src/main/resources/application.properties, application-dev.yml, application-prod.yml
- Security: src/main/java/com/ecommerce/platform/config/SecurityConfig.java, security/JwtTokenProvider.java
- Tests: src/test/java/com/ecommerce/platform/

## Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| JWT_SECRET | JWT signing key | change-me-in-production... |
| DB_URL | Database URL (prod) | localhost:5432 |
| DB_USERNAME | Database username | - |
| DB_PASSWORD | Database password | - |
| ALLOWED_ORIGINS | CORS origins | http://localhost:3000 |

## Status
- Phase 0-5: COMPLETE
- Phase 6: PENDING (agent reviews)
- Phase 7: PENDING (documentation push)
