package com.shushant.hospital_management.modules.patient.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String address,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
