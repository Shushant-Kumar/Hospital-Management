# Hospital Management Backend

Production-oriented backend for a Hospital Management System built with Java, Spring Boot, and PostgreSQL.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security (JWT + RBAC)
- Spring Data JPA
- PostgreSQL (primary)
- H2 (test profile)
- Maven

## Current Implementation Status

Implemented modules and foundations:

- Auth and Security foundation
  - User, role, permission, refresh token, password reset token model
  - JWT access token and refresh token flow
  - Register, login, refresh token, logout, forgot password, reset password APIs
  - Account lock logic after failed logins
  - Rate limiting for sensitive auth endpoints
  - Method-level authorization support
- Department and Doctor module
  - Create and list departments
  - Add, update, soft-delete doctors
  - List doctors by department (paginated)
  - Doctor schedule read API
- Patient module enhancements
  - Soft delete
  - Paginated list
  - Search API

Planned next modules:

- Appointment booking and conflict detection
- Medical records and prescriptions
- Billing and payments
- Admin dashboard, staff management, and audit logs

## Project Structure

Main source root:

- src/main/java/com/shushant/hospital_management

Important top-level areas:

- common: shared dto, entity base, exception handling, utilities
- security: security configuration, JWT filters/services, user principal loading
- modules/auth: auth domain and APIs
- modules/patient: patient APIs and services
- modules/department: department APIs and services
- modules/doctor: doctor APIs and services
- modules/appointment: appointment entities and repository (foundation)

## Getting Started

## 1) Prerequisites

- JDK 21+
- Maven (or use Maven Wrapper)
- PostgreSQL 14+

## 2) Configure Environment

Use environment variables. Do not commit secrets.

Required:

- DB_URL
- DB_USERNAME
- DB_PASSWORD
- JWT_SECRET

Useful optional values:

- SERVER_PORT
- JWT_ACCESS_TOKEN_MINUTES
- JWT_REFRESH_TOKEN_DAYS

You can copy .env.example to .env for local convenience.

## 3) Run the Application

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Compile only:

```powershell
.\mvnw.cmd -DskipTests compile
```

Run tests:

```powershell
.\mvnw.cmd test
```

## API Base Path

All HTTP APIs use:

- /api/v1

Examples:

- /api/v1/auth/login
- /api/v1/patients
- /api/v1/departments
- /api/v1/doctors

## Security Notes

- JWT is used for stateless authentication.
- Access tokens are short-lived.
- Refresh tokens are persisted and rotated.
- Passwords are BCrypt-hashed.
- RBAC is enforced with roles and permissions.

## Configuration Notes

Main configuration file:

- src/main/resources/application.properties

This project currently uses Hibernate auto schema update in local development.
For production, use explicit migration tooling (Flyway or Liquibase) and controlled rollout practices.

## Design Documentation

Detailed architecture is documented in:

- ARCHITECTURE.md

Local setup references and notes are in:

- HELP.md

## License

This project is licensed under the MIT License. See LICENSE.
