package com.shushant.hospital_management.util;

import com.shushant.hospital_management.dao.AppointmentDao;
import com.shushant.hospital_management.dao.LabTestDao;
import com.shushant.hospital_management.dao.PatientDao;

/**
 * Provides robust backend-level validation to prevent unauthorized access
 * based on Record Ownership logic.
 */
public class SecurityGuard {

    private static final PatientDao patientDao = new PatientDao();
    private static final AppointmentDao appointmentDao = new AppointmentDao();
    private static final LabTestDao labTestDao = new LabTestDao();

    /**
     * Throws an exception if the current doctor is not assigned to the given patient.
     */
    public static void verifyPatientAssignment(int patientId) {
        if (!RBACManager.isDoctorRole()) return; // Non-doctors are bound by generic view permissions
        int doctorId = SessionManager.getCurrentDoctorId();
        if (doctorId <= 0) throw new SecurityException("Doctor session not found.");

        if (!patientDao.isAssignedToDoctor(patientId, doctorId)) {
            throw new SecurityException("Access Denied: Patient is not assigned to you.");
        }
    }

    /**
     * Throws an exception if the current doctor is not the owner of the appointment.
     */
    public static void verifyAppointmentOwnership(int appointmentId) {
        if (!RBACManager.isDoctorRole()) return;
        int doctorId = SessionManager.getCurrentDoctorId();
        if (doctorId <= 0) throw new SecurityException("Doctor session not found.");

        if (!appointmentDao.belongsToDoctor(appointmentId, doctorId)) {
            throw new SecurityException("Access Denied: You do not own this appointment.");
        }
    }

    /**
     * Throws an exception if the current doctor is not the one who ordered the lab test.
     */
    public static void verifyLabTestOwnership(int labTestId) {
        if (!RBACManager.isDoctorRole()) return;
        int doctorId = SessionManager.getCurrentDoctorId();
        if (doctorId <= 0) throw new SecurityException("Doctor session not found.");

        if (!labTestDao.isOrderedByDoctor(labTestId, doctorId)) {
            throw new SecurityException("Access Denied: You did not order this lab test.");
        }
    }
}
