# 🏥 Hospital Management System

A production-grade **Java Swing Desktop Application** for managing hospital operations — built with **FlatLaf** dark theme, **PostgreSQL** (via Docker), **HikariCP** connection pooling, and **BCrypt** security.

---

## 📋 Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| **Java JDK** | 21+ | `java -version` |
| **Docker** | Any | `docker --version` |

> Maven wrapper (`mvnw.cmd`) is included — no separate Maven install needed.

---

## 🚀 How to Run (Step-by-Step)

### Step 1 — Start PostgreSQL (Docker)

```bash
docker compose up -d
```

This creates a PostgreSQL 16 container named `hms-postgres` on port **5432** with:
- **Database:** `hospital_management`
- **User:** `postgres`
- **Password:** `Saraswati123`

Verify it's running:
```bash
docker ps
```

### Step 2 — Build & Launch the Application

```bash
.\mvnw.cmd compile exec:java -D"exec.mainClass=com.shushant.hospital_management.HospitalManagementApp"
```

On first launch, the app will:
1. Connect to PostgreSQL
2. Create all 14 database tables
3. Seed dummy data (patients, doctors, appointments, etc.)
4. Open the **Login** window

### Step 3 — Login

Use any of these credentials:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | **Admin** (full access) |
| `doctor1` | `doctor123` | Doctor |
| `nurse1` | `nurse123` | Nurse |
| `pharma1` | `pharma123` | Pharmacist |
| `lab1` | `lab123` | Lab Technician |
| `reception1` | `recep123` | Receptionist |

---

## 🧩 Modules

| Module | Features |
|--------|----------|
| **📊 Dashboard** | Live stats — patient count, doctors, today's appointments, bed occupancy, revenue |
| **🧑‍🦽 Patients** | Register, search, edit, delete, view details (15 fields including insurance & emergency) |
| **👨‍⚕️ Doctors** | Add/edit/delete doctors, department assignment, consultation fees |
| **📅 Appointments** | Book with conflict detection, token numbers, check-in/complete/cancel workflows |
| **💳 Billing** | Create bills (amount/discount/tax), record payments (Cash/Card/UPI/NEFT) |
| **💊 Pharmacy** | Medicine inventory CRUD, low stock alerts, batch tracking |
| **🔬 Lab Tests** | Order → Collect Sample → Process → Enter Result workflow |
| **🛏️ Beds & Wards** | Add beds, assign/release patients, ward occupancy tracking |
| **👥 Users** | Create staff accounts, toggle active, reset passwords (Admin only) |

---

## 📦 Seed Data Included

On first launch, the app auto-seeds:

- **6 Users** (admin + 5 staff across roles)
- **10 Departments** (Cardiology, Surgery, Pediatrics, etc.)
- **6 Doctors** (across specializations, ₹500–₹1000 fees)
- **10 Patients** (with insurance, allergies, emergency contacts)
- **8 Appointments** (today, various statuses)
- **12 Medicines** (tablets, capsules, syrups, injections)
- **5 Bills** (paid, partial, pending)
- **6 Lab Tests** (various stages of the workflow)
- **10 Beds** (General, Semi-Private, Private, ICU, NICU)

---

## 🛑 How to Stop

```bash
# Stop the Java app: Close the window or press Ctrl+C in terminal

# Stop PostgreSQL container (keeps data):
docker compose down

# Stop and DELETE all data:
docker compose down -v
```

---

## 📂 Project Structure

```
hospital-management/
├── docker-compose.yml              ← PostgreSQL container config
├── pom.xml                         ← Maven dependencies
├── mvnw.cmd / mvnw                 ← Maven wrapper
└── src/main/java/com/shushant/hospital_management/
    ├── HospitalManagementApp.java  ← Entry point
    ├── db/
    │   ├── DatabaseConnection.java ← HikariCP pool
    │   └── DatabaseInitializer.java← DDL + seed data
    ├── dao/                        ← Data Access Objects (JDBC)
    │   ├── PatientDao, DoctorDao, DepartmentDao
    │   ├── AppointmentDao, BillingDao
    │   ├── PharmacyDao, LabTestDao, BedDao, UserDao
    ├── ui/
    │   ├── LoginFrame.java         ← Login screen
    │   ├── MainFrame.java          ← Sidebar + panel switching
    │   └── panels/                 ← 9 module panels
    └── util/
        └── SessionManager.java     ← BCrypt auth + roles
```

---

## ⚙️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| UI | Swing + FlatLaf 3.5.4 (Dark Theme) |
| Database | PostgreSQL 16 (Docker) |
| Connection Pool | HikariCP 6.2.1 |
| Security | BCrypt (jBCrypt 0.4) |
| Build | Maven |
