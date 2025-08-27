# Backend сервис Портала Волонтера

### Зависимости приложения
Для локального запуска достаточно (см `docker-compose.yml`):
1. PostgreSQL - для хранения данных
2. Redis - для хранения пользовательских сессий
3. S3 - для хранения

На production среде также есть:
4. Authentik - для авторизации пользователей по технологии Single Sign-on
5. Wordpress - туда синхронизируются пользователи (посколько основной сайт на WP)
6. Cloudflare Turnstile - для проверки Captchа токенов
7. SMTP сервер - для отправки e-mail

### Локальный запуск
`docker-compose up -d` (без `-d` чтобы смотреть сразу логи)  
Запускать main в файле `PortalBackendApplication.kt`  
Указывать spring-профили `local,no-auth`  

### Быстрый старт
api лежит в `api/openapi.yaml` и генерится автоматически при билде или при запуске gradle таски `api/openapi.yaml`  
Этот файл можно перетащить в postman и он сгенерит удобно api для тестирования, останется только прописать baseUrl = `http://localhost:8081/` в переменные постмана

### Тесты 
- Для запуска использовать docker-compose-test.
- Указывать профили "local", "no-auth", "test"

Описание от нейросети:

# Portal Backend - Project Overview

## Project Description
Portal Backend is a Kotlin/Spring Boot application that serves as the backend service for a volunteer portal system ("Портал Волонтера"). It's designed as a comprehensive system for managing volunteer applications, user accounts, reports, and integrations with external services.

## Technology Stack

### Core Technologies
- **Language**: Kotlin 2.1.0
- **Framework**: Spring Boot 3.3.6
- **Java Version**: JDK 21
- **Build Tool**: Gradle 8.11.1
- **Database**: PostgreSQL 15
- **ORM**: Hibernate 6.6 with JPA
- **Migration**: Liquibase
- **Caching**: Redis (sessions), Caffeine (in-memory)
- **Container**: Docker with Alpine Linux

### Key Dependencies
- **Security**: Spring Security with OAuth2 (Authentik SSO)
- **API Documentation**: OpenAPI 3.0.1 with code generation
- **File Storage**: AWS S3 SDK
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, MockK, Testcontainers, Instancio
- **Monitoring**: Spring Actuator
- **Email**: Spring Boot Mail with Thymeleaf templates
- **Serialization**: Jackson with Kotlin module, kotlinx-serialization
- **HTTP Client**: OkHttp 5.0.0-alpha.14
- **Concurrency**: Kotlin Coroutines
- **Scheduled Tasks**: ShedLock for distributed locking
- **Data Export**: OpenCSV, Jackson CSV
- **Logging**: Logback with Logstash encoder

## Project Structure

### Multi-Module Architecture
```
portal-backend/
├── service/                 # Main application module
├── api/
│   ├── server-api/         # Generated server API from OpenAPI spec
│   ├── authentik-api/      # Authentik service client
│   ├── wordpress-api/      # WordPress integration client  
│   ├── outline-api/        # Outline knowledge base client
│   └── axios-client/       # Frontend TypeScript client
└── volumes/                # Docker volumes for local development
```

### Domain Organization (DDD-style)
The service follows Domain-Driven Design principles with clear module boundaries:

- **Application**: Volunteer application management
- **User**: Account management and synchronization
- **Report**: Report submission and processing
- **Program/Project**: Program and project management
- **File**: File upload and S3 storage management
- **Mail**: Email notifications and outbox pattern
- **Note**: Note/comment system
- **Shared**: Common utilities, JPA abstractions, security, auditing

Each domain module contains:
- `api/`: REST controllers
- `domain/`: JPA entities, enums, listeners, specifications
- `service/`: Business logic
- `repository/`: Data access layer
- `mapper/`: DTO mapping (MapStruct)
- `event/`: Domain events
- `scheduler/`: Scheduled tasks

## Database & Persistence

### Database Management
- **Primary DB**: PostgreSQL with connection pooling (HikariCP)
- **Schema Management**: Liquibase with versioned migrations
- **Schema Structure**: Organized by releases (1.0.0 through 1.7.0)
- **JPA Enhancement**: Hibernate lazy loading, dirty tracking
- **Audit Trail**: Comprehensive audit logging system
- **Session Storage**: Redis for distributed sessions

### Key Entities
- **Account**: User accounts with UserInfo, contracts, and group memberships
- **Application**: Volunteer applications with status tracking
- **Report**: User reports with task management
- **Program/Project**: Organizational structure
- **FileInfo**: File metadata and S3 integration
- **EmailOutbox**: Reliable email delivery with retry logic

## External Integrations

### Authentication & Authorization
- **Authentik**: Single Sign-On provider with OAuth2/OIDC
- **Security Profiles**: `no-auth` profile for local development
- **Session Management**: Redis-backed sessions with 48h timeout

### Third-Party Services
- **WordPress**: User synchronization with multiple WP instances
- **Outline**: Knowledge base integration for documentation
- **Cloudflare Turnstile**: CAPTCHA validation
- **SMTP**: Email notifications
- **S3**: File storage (MinIO for local development)

## Configuration & Profiles

### Spring Profiles
- **local**: Local development with Docker Compose services
- **no-auth**: Disable authentication for development
- **test**: Test configuration with in-memory setup

### Environment Configuration
The application uses environment variables for all external dependencies:
- Database: `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS`
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASS`, `REDIS_DATABASE`
- S3: `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_ENDPOINT`, `S3_BUCKET`
- OAuth2: `OAUTH2_SERVER`, `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET`
- External APIs: `AUTHENTIK_API_KEY`, `OUTLINE_API_KEY`
- Email: `SMTP_SERVER`, `SMTP_USERNAME`, `SMTP_PASSWORD`
- Security: `TURNSTILE_SITE_KEY`, `TURNSTILE_SECRET`

## Development Workflow

### Local Development Setup
```bash
# 1. Start dependencies
docker-compose up -d

# 2. Run application with profiles
./gradlew bootRun --args='--spring.profiles.active=local,no-auth'

# 3. API testing
# Import api/openapi.yaml into Postman
# Set baseUrl = http://localhost:8081/
```

### Testing
```bash
# Unit tests
./gradlew test

# Integration tests with test containers
./gradlew test --args='--spring.profiles.active=local,no-auth,test'
```

### Build & Deployment
```bash
# Build all modules including API generation
./gradlew clean build

# Docker build
docker build -t portal-backend .

# The Dockerfile uses multi-stage build with Gradle cache optimization
```

## API Design

### OpenAPI Specification
- **Location**: `/api/openapi.yaml`
- **Version**: 1.20.0
- **Code Generation**: Automatic client/server code generation via Gradle
- **Documentation**: Self-documenting with OpenAPI 3.0.1

### REST API Patterns
- RESTful endpoints with standard HTTP methods
- Consistent error handling with global exception handler
- Request/Response DTOs with validation
- Pagination support for list endpoints
- File upload endpoints with multipart support

## Observability & Operations

### Monitoring
- **Health Checks**: Spring Actuator endpoints
- **Logging**: Structured logging with Logstash format
- **Request Logging**: Configurable request/response logging
- **Async Request Timeout**: 30-second timeout configuration

### Scheduled Jobs
- **Email Outbox**: Every 15 seconds (`*/15 * * * * *`)
- **User Sync**: Hourly (`0 0 */1 * * *`)
- **Expired Applications**: Daily at 6 AM (`0 0 6 */1 * *`)

### Error Handling
- Global exception handler for consistent error responses
- Custom exception types for business logic violations
- Client disconnection handling to prevent resource leaks
- Validation error mapping

## Security Considerations

### Authentication & Authorization
- OAuth2/OIDC integration with Authentik
- JWT token validation
- Role-based access control via UserGroup enum
- Session management with secure cookies

### Data Protection
- Input validation with Bean Validation
- SQL injection prevention via JPA/Hibernate
- File upload restrictions (5MB limit, type validation)
- CAPTCHA integration for public endpoints

## Performance Optimizations

### Caching Strategy
- **Redis**: Distributed session storage
- **Caffeine**: In-memory caching for frequently accessed data
- **JPA**: Query optimization with entity graphs and specifications

### Database Optimizations
- Connection pooling with HikariCP
- Lazy loading with Hibernate enhancement
- Batch processing for bulk operations
- Proper indexing strategy (managed via Liquibase)

### Async Processing
- Email outbox pattern for reliable message delivery
- Scheduled task processing with ShedLock for coordination
- Coroutines for non-blocking operations

## Development Guidelines

### Code Organization
- Follow domain-driven design principles
- Use dependency injection with Spring's component model
- Implement proper separation of concerns (Controller → Service → Repository)
- Use MapStruct for DTO mapping to avoid boilerplate

### Testing Strategy
- Unit tests with MockK for mocking
- Integration tests with Testcontainers for real database testing
- Test data generation with Instancio and Kotlin Faker
- Profile-specific test configurations

### Code Quality
- Kotlin idioms and null safety
- No explicit linting tools configured (detekt/ktlint not found)
- MapStruct validation with ERROR policy for unmapped targets
- Comprehensive error handling patterns

---

This portal backend serves as a robust foundation for volunteer management with comprehensive integration capabilities, proper security measures, and scalable architecture patterns.
