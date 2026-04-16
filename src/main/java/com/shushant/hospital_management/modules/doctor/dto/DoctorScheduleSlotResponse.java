package com.shushant.hospital_management.modules.doctor.dto;

import com.shushant.hospital_management.modules.appointment.entity.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorScheduleSlotResponse(
        UUID appointmentId,
        UUID patientId,
        String patientName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status) {
}
