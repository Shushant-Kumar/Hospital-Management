package com.shushant.hospital_management.modules.patient.service.impl;

import com.shushant.hospital_management.common.exception.ResourceNotFoundException;
import com.shushant.hospital_management.modules.patient.dto.PatientCreateRequest;
import com.shushant.hospital_management.modules.patient.dto.PatientResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientUpdateRequest;
import com.shushant.hospital_management.modules.patient.entity.Patient;
import com.shushant.hospital_management.modules.patient.mapper.PatientMapper;
import com.shushant.hospital_management.modules.patient.repository.PatientRepository;
import com.shushant.hospital_management.modules.patient.service.PatientService;
import com.shushant.hospital_management.modules.patient.validation.PatientBusinessValidator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PatientBusinessValidator patientBusinessValidator;

    public PatientServiceImpl(
            PatientRepository patientRepository,
            PatientMapper patientMapper,
            PatientBusinessValidator patientBusinessValidator) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
        this.patientBusinessValidator = patientBusinessValidator;
    }

    @Override
    public PatientResponse createPatient(PatientCreateRequest request) {
        patientBusinessValidator.validateUniqueForCreate(request.email(), request.phoneNumber());

        Patient patient = patientMapper.toEntity(request);
        Patient savedPatient = patientRepository.save(patient);

        return patientMapper.toResponse(savedPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID patientId) {
        Patient patient = findByIdOrThrow(patientId);
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream().map(patientMapper::toResponse).toList();
    }

    @Override
    public PatientResponse updatePatient(UUID patientId, PatientUpdateRequest request) {
        Patient existingPatient = findByIdOrThrow(patientId);
        patientBusinessValidator.validateUniqueForUpdate(patientId, request.email(), request.phoneNumber());

        patientMapper.updateEntity(existingPatient, request);
        Patient updatedPatient = patientRepository.save(existingPatient);

        return patientMapper.toResponse(updatedPatient);
    }

    @Override
    public void deletePatient(UUID patientId) {
        Patient existingPatient = findByIdOrThrow(patientId);
        patientRepository.delete(existingPatient);
    }

    private Patient findByIdOrThrow(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }
}
