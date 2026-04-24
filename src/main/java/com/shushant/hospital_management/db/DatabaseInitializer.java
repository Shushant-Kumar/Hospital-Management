package com.shushant.hospital_management.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id SERIAL PRIMARY KEY,
                            username VARCHAR(50) UNIQUE NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            full_name VARCHAR(120) NOT NULL,
                            email VARCHAR(120),
                            role VARCHAR(30) NOT NULL DEFAULT 'RECEPTIONIST',
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS departments (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100) UNIQUE NOT NULL,
                            code VARCHAR(30) UNIQUE NOT NULL,
                            description VARCHAR(255),
                            active BOOLEAN NOT NULL DEFAULT TRUE
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS doctors (
                            id SERIAL PRIMARY KEY,
                            first_name VARCHAR(80) NOT NULL,
                            last_name VARCHAR(80) NOT NULL,
                            email VARCHAR(120) UNIQUE NOT NULL,
                            phone VARCHAR(20) UNIQUE NOT NULL,
                            specialization VARCHAR(120) NOT NULL,
                            license_number VARCHAR(50) UNIQUE NOT NULL,
                            department_id INT REFERENCES departments(id),
                            user_id INT REFERENCES users(id),
                            consultation_fee NUMERIC(10,2) DEFAULT 0,
                            consultation_duration_min INT DEFAULT 30,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS patients (
                            id SERIAL PRIMARY KEY,
                            patient_uid VARCHAR(20) UNIQUE NOT NULL,
                            first_name VARCHAR(80) NOT NULL,
                            last_name VARCHAR(80) NOT NULL,
                            email VARCHAR(120),
                            phone VARCHAR(20) NOT NULL,
                            date_of_birth DATE,
                            gender VARCHAR(10),
                            blood_group VARCHAR(5),
                            address VARCHAR(255),
                            patient_type VARCHAR(15) DEFAULT 'OPD',
                            allergies VARCHAR(500),
                            insurance_provider VARCHAR(120),
                            insurance_policy VARCHAR(50),
                            emergency_contact_name VARCHAR(100),
                            emergency_contact_phone VARCHAR(20),
                            user_id INT REFERENCES users(id),
                            created_by INT REFERENCES users(id),
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS appointments (
                            id SERIAL PRIMARY KEY,
                            patient_id INT REFERENCES patients(id),
                            doctor_id INT REFERENCES doctors(id),
                            appointment_date DATE NOT NULL,
                            start_time TIME NOT NULL,
                            end_time TIME NOT NULL,
                            token_number INT DEFAULT 0,
                            status VARCHAR(20) DEFAULT 'BOOKED',
                            notes VARCHAR(500),
                            cancel_reason VARCHAR(255),
                            walk_in BOOLEAN DEFAULT FALSE,
                            created_by INT REFERENCES users(id),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS medical_records (
                            id SERIAL PRIMARY KEY,
                            patient_id INT REFERENCES patients(id),
                            doctor_id INT REFERENCES doctors(id),
                            appointment_id INT REFERENCES appointments(id),
                            diagnosis TEXT,
                            notes TEXT,
                            vitals_bp VARCHAR(20),
                            vitals_temp VARCHAR(10),
                            vitals_pulse VARCHAR(10),
                            vitals_weight VARCHAR(10),
                            vitals_spo2 VARCHAR(10),
                            record_type VARCHAR(25) DEFAULT 'CONSULTATION',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS prescriptions (
                            id SERIAL PRIMARY KEY,
                            patient_id INT REFERENCES patients(id),
                            doctor_id INT REFERENCES doctors(id),
                            appointment_id INT REFERENCES appointments(id),
                            notes VARCHAR(500),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS prescription_items (
                            id SERIAL PRIMARY KEY,
                            prescription_id INT REFERENCES prescriptions(id) ON DELETE CASCADE,
                            medicine_name VARCHAR(150) NOT NULL,
                            dosage VARCHAR(50),
                            frequency VARCHAR(50),
                            duration VARCHAR(50),
                            quantity INT DEFAULT 0,
                            instructions VARCHAR(300)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS bills (
                            id SERIAL PRIMARY KEY,
                            bill_number VARCHAR(30) UNIQUE NOT NULL,
                            patient_id INT REFERENCES patients(id),
                            total_amount NUMERIC(12,2) DEFAULT 0,
                            discount NUMERIC(12,2) DEFAULT 0,
                            tax_amount NUMERIC(12,2) DEFAULT 0,
                            net_amount NUMERIC(12,2) DEFAULT 0,
                            paid_amount NUMERIC(12,2) DEFAULT 0,
                            status VARCHAR(20) DEFAULT 'PENDING',
                            bill_type VARCHAR(30) DEFAULT 'CONSULTATION',
                            created_by INT REFERENCES users(id),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS bill_items (
                            id SERIAL PRIMARY KEY,
                            bill_id INT REFERENCES bills(id) ON DELETE CASCADE,
                            description VARCHAR(255) NOT NULL,
                            quantity INT DEFAULT 1,
                            unit_price NUMERIC(10,2) DEFAULT 0,
                            amount NUMERIC(10,2) DEFAULT 0,
                            category VARCHAR(50)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS payments (
                            id SERIAL PRIMARY KEY,
                            bill_id INT REFERENCES bills(id),
                            amount NUMERIC(12,2) NOT NULL,
                            payment_method VARCHAR(30) DEFAULT 'CASH',
                            transaction_ref VARCHAR(100),
                            payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS medicines (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(150) NOT NULL,
                            generic_name VARCHAR(150),
                            manufacturer VARCHAR(120),
                            batch_number VARCHAR(50),
                            expiry_date DATE,
                            quantity INT DEFAULT 0,
                            unit_price NUMERIC(10,2) DEFAULT 0,
                            reorder_level INT DEFAULT 10,
                            category VARCHAR(50),
                            created_by INT REFERENCES users(id),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS lab_tests (
                            id SERIAL PRIMARY KEY,
                            patient_id INT REFERENCES patients(id),
                            doctor_id INT REFERENCES doctors(id),
                            test_name VARCHAR(150) NOT NULL,
                            test_code VARCHAR(30),
                            status VARCHAR(25) DEFAULT 'BOOKED',
                            sample_type VARCHAR(50),
                            result TEXT,
                            normal_range VARCHAR(100),
                            technician_name VARCHAR(120),
                            created_by INT REFERENCES users(id),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS beds (
                            id SERIAL PRIMARY KEY,
                            ward_name VARCHAR(80) NOT NULL,
                            bed_number VARCHAR(20) NOT NULL,
                            room_type VARCHAR(30) DEFAULT 'GENERAL',
                            floor INT DEFAULT 1,
                            status VARCHAR(20) DEFAULT 'AVAILABLE',
                            patient_id INT REFERENCES patients(id),
                            daily_rate NUMERIC(10,2) DEFAULT 0,
                            created_by INT REFERENCES users(id),
                            UNIQUE(ward_name, bed_number)
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS audit_logs (
                            id SERIAL PRIMARY KEY,
                            action VARCHAR(50) NOT NULL,
                            entity_name VARCHAR(50) NOT NULL,
                            entity_id INT NOT NULL,
                            user_id INT REFERENCES users(id),
                            details TEXT,
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Seed default admin if not exists
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setString(1, "admin");
                        ps.setString(2, hash);
                        ps.setString(3, "System Administrator");
                        ps.setString(4, "admin@hospital.com");
                        ps.setString(5, "ADMIN");
                        ps.executeUpdate();
                    }
                }
            }

            // Seed departments if empty
            stmt.execute("""
                        INSERT INTO departments (name, code, description)
                        SELECT * FROM (VALUES
                            ('General Medicine', 'MED', 'Internal Medicine'),
                            ('Surgery', 'SUR', 'General Surgery'),
                            ('Pediatrics', 'PED', 'Child Healthcare'),
                            ('Orthopedics', 'ORT', 'Bone and Joint'),
                            ('Cardiology', 'CAR', 'Heart and Cardiovascular'),
                            ('Neurology', 'NEU', 'Brain and Nervous System'),
                            ('Dermatology', 'DER', 'Skin Care'),
                            ('ENT', 'ENT', 'Ear Nose Throat'),
                            ('Ophthalmology', 'OPH', 'Eye Care'),
                            ('Gynecology', 'GYN', 'Women Health')
                        ) AS v(name, code, description)
                        WHERE NOT EXISTS (SELECT 1 FROM departments LIMIT 1)
                    """);

            // ── Seed additional staff users ──
            seedUser(conn, "doctor1", "doctor123", "Dr. Rajesh Kumar", "rajesh@hospital.com", "DOCTOR");
            seedUser(conn, "nurse1", "nurse123", "Priya Sharma", "priya@hospital.com", "NURSE");
            seedUser(conn, "pharma1", "pharma123", "Amit Patel", "amit@hospital.com", "PHARMACIST");
            seedUser(conn, "lab1", "lab123", "Sneha Gupta", "sneha@hospital.com", "LAB_TECHNICIAN");
            seedUser(conn, "reception1", "recep123", "Kavita Singh", "kavita@hospital.com", "RECEPTIONIST");
            seedUser(conn, "patient1", "patient123", "Arjun Verma", "arjun.v@email.com", "PATIENT");
            seedUser(conn, "accountant1", "account123", "Ravi Mehta", "ravi@hospital.com", "ACCOUNTANT");

            // ── Seed dummy doctors ──
            stmt.execute(
                    """
                                INSERT INTO doctors (first_name, last_name, email, phone, specialization, license_number, department_id, consultation_fee, consultation_duration_min)
                                SELECT * FROM (VALUES
                                    ('Rajesh', 'Kumar', 'dr.rajesh@hospital.com', '9876543210', 'Cardiology', 'LIC-CAR-001', 5, 800.00, 30),
                                    ('Anita', 'Desai', 'dr.anita@hospital.com', '9876543211', 'Pediatrics', 'LIC-PED-001', 3, 600.00, 20),
                                    ('Vikram', 'Mehta', 'dr.vikram@hospital.com', '9876543212', 'Orthopedics', 'LIC-ORT-001', 4, 900.00, 30),
                                    ('Sunita', 'Rao', 'dr.sunita@hospital.com', '9876543213', 'Neurology', 'LIC-NEU-001', 6, 1000.00, 45),
                                    ('Mohan', 'Iyer', 'dr.mohan@hospital.com', '9876543214', 'General Medicine', 'LIC-MED-001', 1, 500.00, 20),
                                    ('Priyanka', 'Shah', 'dr.priyanka@hospital.com', '9876543215', 'Dermatology', 'LIC-DER-001', 7, 700.00, 25)
                                ) AS v(fn, ln, em, ph, spec, lic, dept, fee, dur)
                                WHERE NOT EXISTS (SELECT 1 FROM doctors LIMIT 1)
                            """);

            // ── Seed dummy patients ──
            stmt.execute(
                    """
                                INSERT INTO patients (patient_uid, first_name, last_name, email, phone, date_of_birth, gender, blood_group, address, patient_type, allergies, insurance_provider, insurance_policy, emergency_contact_name, emergency_contact_phone)
                                SELECT * FROM (VALUES
                                    ('PAT-20250001', 'Arjun', 'Verma', 'arjun.v@email.com', '8001000001', '1990-05-15'::DATE, 'MALE', 'A+', '12 MG Road, Delhi', 'OPD', 'Penicillin', 'Star Health', 'SH-90001', 'Meera Verma', '8001000099'),
                                    ('PAT-20250002', 'Neha', 'Sharma', 'neha.s@email.com', '8001000002', '1985-11-20'::DATE, 'FEMALE', 'B+', '45 Park Street, Mumbai', 'OPD', 'None', 'ICICI Lombard', 'IL-90002', 'Rakesh Sharma', '8001000098'),
                                    ('PAT-20250003', 'Rahul', 'Joshi', 'rahul.j@email.com', '8001000003', '1978-03-10'::DATE, 'MALE', 'O+', '78 Lake Road, Kolkata', 'IPD', 'Sulfa Drugs', 'Max Bupa', 'MB-90003', 'Suman Joshi', '8001000097'),
                                    ('PAT-20250004', 'Pooja', 'Nair', 'pooja.n@email.com', '8001000004', '1995-07-25'::DATE, 'FEMALE', 'AB+', '22 Church Street, Bangalore', 'OPD', 'None', '', '', 'Suresh Nair', '8001000096'),
                                    ('PAT-20250005', 'Amit', 'Saxena', 'amit.s@email.com', '8001000005', '1982-01-30'::DATE, 'MALE', 'A-', '56 Civil Lines, Jaipur', 'EMERGENCY', 'Aspirin', 'Star Health', 'SH-90005', 'Rita Saxena', '8001000095'),
                                    ('PAT-20250006', 'Sneha', 'Patil', 'sneha.p@email.com', '8001000006', '2000-09-12'::DATE, 'FEMALE', 'B-', '33 FC Road, Pune', 'OPD', 'None', '', '', 'Ramesh Patil', '8001000094'),
                                    ('PAT-20250007', 'Karthik', 'Reddy', 'karthik.r@email.com', '8001000007', '1970-12-05'::DATE, 'MALE', 'O-', '90 Jubilee Hills, Hyderabad', 'IPD', 'Ibuprofen', 'HDFC Ergo', 'HE-90007', 'Lakshmi Reddy', '8001000093'),
                                    ('PAT-20250008', 'Divya', 'Menon', 'divya.m@email.com', '8001000008', '1988-04-18'::DATE, 'FEMALE', 'AB-', '15 Marine Drive, Kochi', 'OPD', 'None', 'Bajaj Allianz', 'BA-90008', 'Anil Menon', '8001000092'),
                                    ('PAT-20250009', 'Suresh', 'Yadav', 'suresh.y@email.com', '8001000009', '1965-06-22'::DATE, 'MALE', 'A+', '67 Mall Road, Lucknow', 'IPD', 'Codeine', 'Star Health', 'SH-90009', 'Geeta Yadav', '8001000091'),
                                    ('PAT-20250010', 'Ananya', 'Bose', 'ananya.b@email.com', '8001000010', '2005-02-14'::DATE, 'FEMALE', 'B+', '11 Salt Lake, Kolkata', 'OPD', 'None', '', '', 'Tapan Bose', '8001000090')
                                ) AS v(uid, fn, ln, em, ph, dob, gen, bg, addr, pt, alg, ins, pol, ecn, ecp)
                                WHERE NOT EXISTS (SELECT 1 FROM patients LIMIT 1)
                            """);

            // ── Seed dummy appointments (today) ──
            stmt.execute(
                    """
                                INSERT INTO appointments (patient_id, doctor_id, appointment_date, start_time, end_time, token_number, status, notes, walk_in)
                                SELECT * FROM (VALUES
                                    (1, 1, CURRENT_DATE, '09:00'::TIME, '09:30'::TIME, 1, 'BOOKED', 'Routine cardiac checkup', FALSE),
                                    (2, 2, CURRENT_DATE, '09:00'::TIME, '09:20'::TIME, 1, 'CHECKED_IN', 'Child vaccination', FALSE),
                                    (3, 3, CURRENT_DATE, '10:00'::TIME, '10:30'::TIME, 1, 'BOOKED', 'Knee pain follow-up', FALSE),
                                    (4, 5, CURRENT_DATE, '10:00'::TIME, '10:20'::TIME, 1, 'COMPLETED', 'General health checkup', FALSE),
                                    (5, 1, CURRENT_DATE, '11:00'::TIME, '11:30'::TIME, 2, 'BOOKED', 'Emergency chest pain evaluation', TRUE),
                                    (6, 6, CURRENT_DATE, '11:00'::TIME, '11:25'::TIME, 1, 'BOOKED', 'Skin rash consultation', FALSE),
                                    (7, 4, CURRENT_DATE, '14:00'::TIME, '14:45'::TIME, 1, 'BOOKED', 'Migraine follow-up', FALSE),
                                    (8, 5, CURRENT_DATE, '14:00'::TIME, '14:20'::TIME, 2, 'BOOKED', 'Fever and cold', TRUE)
                                ) AS v(pid, did, dt, st, et, tok, stat, notes, wi)
                                WHERE NOT EXISTS (SELECT 1 FROM appointments LIMIT 1)
                            """);

            // ── Seed dummy medicines ──
            stmt.execute(
                    """
                                INSERT INTO medicines (name, generic_name, manufacturer, batch_number, expiry_date, quantity, unit_price, reorder_level, category)
                                SELECT * FROM (VALUES
                                    ('Crocin 650', 'Paracetamol', 'GSK', 'BCH-001', '2027-06-30'::DATE, 500, 12.50, 50, 'TABLET'),
                                    ('Amoxicillin 500', 'Amoxicillin', 'Cipla', 'BCH-002', '2026-12-31'::DATE, 300, 8.00, 30, 'CAPSULE'),
                                    ('Pan-D', 'Pantoprazole', 'Alkem', 'BCH-003', '2027-03-15'::DATE, 200, 15.00, 20, 'CAPSULE'),
                                    ('Augmentin 625', 'Amoxiclav', 'GSK', 'BCH-004', '2026-09-30'::DATE, 150, 28.50, 20, 'TABLET'),
                                    ('Dolo 650', 'Paracetamol', 'Micro Labs', 'BCH-005', '2027-08-20'::DATE, 800, 10.00, 100, 'TABLET'),
                                    ('Azithromycin 500', 'Azithromycin', 'Cipla', 'BCH-006', '2026-11-15'::DATE, 100, 65.00, 15, 'TABLET'),
                                    ('Benadryl Cough Syrup', 'Diphenhydramine', 'J&J', 'BCH-007', '2027-01-20'::DATE, 80, 95.00, 10, 'SYRUP'),
                                    ('Insulin Mixtard', 'Insulin Human', 'Novo Nordisk', 'BCH-008', '2026-07-15'::DATE, 5, 450.00, 5, 'INJECTION'),
                                    ('Betadine Ointment', 'Povidone-Iodine', 'Win-Medicare', 'BCH-009', '2027-12-31'::DATE, 120, 55.00, 15, 'CREAM'),
                                    ('Combiflam', 'Ibuprofen+Paracetamol', 'Sanofi', 'BCH-010', '2027-04-30'::DATE, 400, 14.00, 50, 'TABLET'),
                                    ('Metformin 500', 'Metformin', 'USV', 'BCH-011', '2027-09-30'::DATE, 350, 6.50, 40, 'TABLET'),
                                    ('Cetirizine 10', 'Cetirizine', 'Dr Reddy', 'BCH-012', '2027-05-15'::DATE, 600, 5.00, 60, 'TABLET')
                                ) AS v(nm, gn, mfg, batch, exp, qty, price, reord, cat)
                                WHERE NOT EXISTS (SELECT 1 FROM medicines LIMIT 1)
                            """);

            // ── Seed dummy bills ──
            stmt.execute(
                    """
                                INSERT INTO bills (bill_number, patient_id, total_amount, discount, tax_amount, net_amount, paid_amount, status, bill_type)
                                SELECT * FROM (VALUES
                                    ('BILL-20250001', 1, 800.00, 0.00, 40.00, 840.00, 840.00, 'PAID', 'CONSULTATION'),
                                    ('BILL-20250002', 2, 600.00, 50.00, 27.50, 577.50, 577.50, 'PAID', 'CONSULTATION'),
                                    ('BILL-20250003', 3, 2500.00, 0.00, 125.00, 2625.00, 1000.00, 'PARTIAL', 'SURGERY'),
                                    ('BILL-20250004', 5, 3200.00, 200.00, 150.00, 3150.00, 0.00, 'PENDING', 'CONSULTATION'),
                                    ('BILL-20250005', 7, 15000.00, 500.00, 725.00, 15225.00, 15225.00, 'PAID', 'SURGERY')
                                ) AS v(bnum, pid, total, disc, tax, net, paid, stat, btype)
                                WHERE NOT EXISTS (SELECT 1 FROM bills LIMIT 1)
                            """);

            // ── Seed dummy payments ──
            stmt.execute("""
                        INSERT INTO payments (bill_id, amount, payment_method, transaction_ref)
                        SELECT * FROM (VALUES
                            (1, 840.00, 'UPI', 'UPI-TXN-98765'),
                            (2, 577.50, 'CARD', 'CARD-TXN-54321'),
                            (3, 1000.00, 'CASH', 'CASH-REC-11111'),
                            (5, 15225.00, 'BANK_TRANSFER', 'NEFT-TXN-77777')
                        ) AS v(bid, amt, method, ref)
                        WHERE NOT EXISTS (SELECT 1 FROM payments LIMIT 1)
                    """);

            // ── Seed dummy lab tests ──
            stmt.execute(
                    """
                                INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_code, status, sample_type, result, normal_range, technician_name)
                                SELECT * FROM (VALUES
                                    (1, 1, 'Complete Blood Count', 'CBC-001', 'COMPLETED', 'BLOOD', 'Hb: 14.2 g/dL, WBC: 7500', 'Hb: 12-16 g/dL', 'Sneha Gupta'),
                                    (3, 3, 'X-Ray Knee', 'XRAY-001', 'COMPLETED', 'OTHER', 'Mild degenerative changes noted', 'Normal joint space', 'Sneha Gupta'),
                                    (5, 1, 'ECG', 'ECG-001', 'PROCESSING', 'OTHER', '', 'Normal sinus rhythm', ''),
                                    (7, 4, 'MRI Brain', 'MRI-001', 'SAMPLE_COLLECTED', 'OTHER', '', '', ''),
                                    (9, 5, 'Blood Sugar Fasting', 'BSF-001', 'COMPLETED', 'BLOOD', 'Glucose: 145 mg/dL', '70-100 mg/dL', 'Sneha Gupta'),
                                    (2, 2, 'Urine Analysis', 'UA-001', 'BOOKED', 'URINE', '', '', '')
                                ) AS v(pid, did, tname, tcode, stat, stype, res, nrange, tech)
                                WHERE NOT EXISTS (SELECT 1 FROM lab_tests LIMIT 1)
                            """);

            // ── Seed dummy beds ──
            stmt.execute("""
                        INSERT INTO beds (ward_name, bed_number, room_type, floor, status, patient_id, daily_rate)
                        SELECT * FROM (VALUES
                            ('General Ward A', 'GA-101', 'GENERAL', 1, 'OCCUPIED', 3, 500.00),
                            ('General Ward A', 'GA-102', 'GENERAL', 1, 'AVAILABLE', NULL, 500.00),
                            ('General Ward A', 'GA-103', 'GENERAL', 1, 'AVAILABLE', NULL, 500.00),
                            ('General Ward B', 'GB-201', 'SEMI_PRIVATE', 2, 'OCCUPIED', 7, 1200.00),
                            ('General Ward B', 'GB-202', 'SEMI_PRIVATE', 2, 'AVAILABLE', NULL, 1200.00),
                            ('Private Ward', 'PV-301', 'PRIVATE', 3, 'OCCUPIED', 9, 3000.00),
                            ('Private Ward', 'PV-302', 'PRIVATE', 3, 'AVAILABLE', NULL, 3000.00),
                            ('ICU', 'ICU-01', 'ICU', 1, 'AVAILABLE', NULL, 5000.00),
                            ('ICU', 'ICU-02', 'ICU', 1, 'OCCUPIED', 5, 5000.00),
                            ('NICU', 'NICU-01', 'NICU', 1, 'AVAILABLE', NULL, 6000.00)
                        ) AS v(ward, bed, room, fl, stat, pid, rate)
                        WHERE NOT EXISTS (SELECT 1 FROM beds LIMIT 1)
                    """);

            // ── Link doctor1 user to doctor record ──
            stmt.execute("""
                        UPDATE doctors SET user_id = (SELECT id FROM users WHERE username = 'doctor1')
                        WHERE email = 'dr.rajesh@hospital.com' AND user_id IS NULL
                    """);

            // ── Link patient1 user to patient record ──
            stmt.execute("""
                        UPDATE patients SET user_id = (SELECT id FROM users WHERE username = 'patient1')
                        WHERE patient_uid = 'PAT-20250001' AND user_id IS NULL
                    """);

            System.out.println("[DB] Schema and seed data initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private static void seedUser(Connection conn, String username, String password,
            String fullName, String email, String role) throws SQLException {
        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    String hash = BCrypt.hashpw(password, BCrypt.gensalt());
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setString(1, username);
                        ps.setString(2, hash);
                        ps.setString(3, fullName);
                        ps.setString(4, email);
                        ps.setString(5, role);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }
}
