package com.shushant.hospital_management.modules.doctor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String specialization,
        String licenseNumber,
        int consultationDurationMinutes,
        boolean active,
        UUID departmentId,
        String departmentName,
        String departmentCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
