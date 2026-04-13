package com.shushant.hospital_management.modules.patient.mapper;

import com.shushant.hospital_management.modules.patient.dto.PatientCreateRequest;
import com.shushant.hospital_management.modules.patient.dto.PatientResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientUpdateRequest;
import com.shushant.hospital_management.modules.patient.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toEntity(PatientCreateRequest request) {
        Patient patient = new Patient();
        applyCommonFields(patient, request.firstName(), request.lastName(), request.email(), request.phoneNumber(),
                request.dateOfBirth(), request.address());
        return patient;
    }

    public void updateEntity(Patient patient, PatientUpdateRequest request) {
        applyCommonFields(patient, request.firstName(), request.lastName(), request.email(), request.phoneNumber(),
                request.dateOfBirth(), request.address());
    }

    public PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getEmail(),
                patient.getPhoneNumber(),
                patient.getDateOfBirth(),
                patient.getAddress(),
                patient.getCreatedAt(),
                patient.getUpdatedAt());
    }

    private void applyCommonFields(
            Patient patient,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            java.time.LocalDate dateOfBirth,
            String address) {
        patient.setFirstName(firstName.trim());
        patient.setLastName(lastName.trim());
        patient.setEmail(email.trim().toLowerCase());
        patient.setPhoneNumber(phoneNumber.trim());
        patient.setDateOfBirth(dateOfBirth);
        patient.setAddress(address.trim());
    }
}
