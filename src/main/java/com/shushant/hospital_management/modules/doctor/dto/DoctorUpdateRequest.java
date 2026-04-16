package com.shushant.hospital_management.modules.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DoctorUpdateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 120) String specialization,
        @NotBlank @Size(max = 50) String licenseNumber,
        @Min(5) int consultationDurationMinutes,
        @NotNull UUID departmentId,
        boolean active) {
}
