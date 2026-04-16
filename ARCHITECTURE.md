# Hospital Management System Backend Architecture

## 1. Target Architecture

- Style: Modular monolith using domain modules under `modules/*`.
- Runtime: Spring Boot 4, Java 21.
- Data: PostgreSQL with relational modeling and UUID primary keys.
- Security: JWT access tokens + refresh token rotation + RBAC + method security.
- Reliability: Database constraints, optimistic locking for high-contention updates, audit trails.
- Scalability path: Extract module boundaries into microservices later using the same contracts.

## 2. Package Strategy

```
com.shushant.hospital_management
  common/
    config/
    dto/
    entity/
    exception/
    util/
  security/
    config/
    jwt/
    user/
  modules/
    auth/
      controller/
      dto/
      entity/
      repository/
      service/
      validation/
    patient/
    doctor/
    department/
    appointment/
    medical/
    billing/
    admin/
```

## 3. Database Design

### Core IAM

- `users`
  - `id` (uuid, pk)
  - `email` (unique, not null)
  - `password_hash` (not null)
  - `first_name`, `last_name`
  - `active` (bool)
  - `account_non_locked` (bool)
  - `failed_login_attempts` (int)
  - `deleted` (bool)
  - `created_at`, `updated_at`
- `roles`
  - `id` (uuid, pk)
  - `name` (unique, ex: `ROLE_ADMIN`)
  - `description`
- `permissions`
  - `id` (uuid, pk)
  - `name` (unique, ex: `PATIENT_READ`)
  - `description`
- `role_permissions`
  - `role_id`, `permission_id` (composite unique)
- `user_roles`
  - `user_id`, `role_id` (composite unique)
- `refresh_tokens`
  - `id` (uuid, pk)
  - `user_id` (fk users)
  - `token_hash` (unique)
  - `expires_at`
  - `revoked_at`
  - `created_at`

### Clinical

- `patients`
  - demographics + contact + emergency contact + soft delete fields
- `departments`
  - `name`, `code`, `head_doctor_id`
- `doctors`
  - profile, specialization, `department_id`, consultation slots, active flags
- `appointments`
  - `patient_id`, `doctor_id`, `start_time`, `end_time`, `status`, `cancel_reason`, `reschedule_ref`
  - uniqueness and overlap checks per doctor/time
- `prescriptions`
  - linked to appointment/patient/doctor
- `medical_records`
  - diagnosis, notes, report metadata, blob/object storage pointer
- `bills`
  - invoice data, line items, payment status

## 4. API Surface (by module)

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

### Patients

- `POST /api/v1/patients`
- `PUT /api/v1/patients/{id}`
- `DELETE /api/v1/patients/{id}`
- `GET /api/v1/patients/{id}`
- `GET /api/v1/patients/search`
- `GET /api/v1/patients`

### Doctors

- `POST /api/v1/doctors`
- `PUT /api/v1/doctors/{id}`
- `DELETE /api/v1/doctors/{id}`
- `GET /api/v1/doctors/{id}/schedule`
- `GET /api/v1/departments/{departmentId}/doctors`

### Appointments

- `POST /api/v1/appointments`
- `PATCH /api/v1/appointments/{id}/cancel`
- `PATCH /api/v1/appointments/{id}/reschedule`
- `GET /api/v1/doctors/{id}/appointments`
- `GET /api/v1/patients/{id}/appointments`

### Medical

- `POST /api/v1/medical/prescriptions`
- `POST /api/v1/medical/diagnoses`
- `POST /api/v1/medical/reports`
- `GET /api/v1/patients/{id}/medical-history`

### Billing

- `POST /api/v1/bills/generate`
- `GET /api/v1/patients/{id}/payments`

### Admin

- `POST /api/v1/admin/staff`
- `PUT /api/v1/admin/users/{id}/roles`
- `GET /api/v1/admin/audit-logs`
- `GET /api/v1/admin/dashboard/stats`

## 5. Security Design

- JWT access token for API calls (short TTL).
- Refresh token in secure cookie or explicit payload (rotated on refresh).
- Passwords hashed with BCrypt.
- Method-level authorization with `@PreAuthorize`.
- CSRF disabled for pure stateless APIs; enabled if cookie-auth UI is introduced.
- CORS strict allowlist by environment.
- Rate limiting and brute-force protection at auth endpoints.
- Login failures tracked per account and temporary lockout enforced.

## 6. Validation and Error Contract

- DTO-driven validation using Jakarta Validation.
- `ApiResponse<T>` for success/failure envelope.
- `ErrorResponse` with machine-readable code + details.
- Global exception handler maps domain exceptions and validation failures consistently.

## 7. Audit Strategy

- `@CreatedDate` + `@LastModifiedDate` for all entities.
- Application audit events for:
  - Role assignment
  - Password reset
  - Patient record changes
  - Appointment cancellation/reschedule
- Persist to `audit_logs` for compliance visibility.

## 8. Module-by-Module Build Sequence

1. Auth + Security foundation (users, roles, permissions, JWT, refresh token).
2. Department + Doctor management.
3. Patient management enhancements (search/pagination/soft delete/security).
4. Appointment scheduling with conflict checks and transaction-safe booking.
5. Medical records and prescriptions.
6. Billing and payment history.
7. Admin APIs (staff/roles/audit/dashboard).
8. Observability hardening (metrics, traces, security telemetry).

Implementation status in workspace:

- Step 1 completed (Auth + security foundation).
- Step 2 completed (Department + Doctor management and schedule read API).
- Step 3 partially completed (Patient pagination/search and soft delete).
