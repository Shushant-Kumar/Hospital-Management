package com.shushant.hospital_management.modules.patient.validation;

import com.shushant.hospital_management.common.exception.ResourceConflictException;
import com.shushant.hospital_management.modules.patient.repository.PatientRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PatientBusinessValidator {

    private final PatientRepository patientRepository;

    public PatientBusinessValidator(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public void validateUniqueForCreate(String email, String phoneNumber) {
        if (patientRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            throw new ResourceConflictException("A patient with this email already exists");
        }

        if (patientRepository.existsByPhoneNumberAndDeletedFalse(phoneNumber)) {
            throw new ResourceConflictException("A patient with this phone number already exists");
        }
    }

    public void validateUniqueForUpdate(UUID patientId, String email, String phoneNumber) {
        if (patientRepository.existsByEmailIgnoreCaseAndIdNotAndDeletedFalse(email, patientId)) {
            throw new ResourceConflictException("A patient with this email already exists");
        }

        if (patientRepository.existsByPhoneNumberAndIdNotAndDeletedFalse(phoneNumber, patientId)) {
            throw new ResourceConflictException("A patient with this phone number already exists");
        }
    }
}
