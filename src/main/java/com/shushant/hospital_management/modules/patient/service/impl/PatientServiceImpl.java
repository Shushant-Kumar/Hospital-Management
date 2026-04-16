package com.shushant.hospital_management.modules.patient.service.impl;

import com.shushant.hospital_management.common.exception.ResourceNotFoundException;
import com.shushant.hospital_management.common.dto.PageResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientCreateRequest;
import com.shushant.hospital_management.modules.patient.dto.PatientResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientUpdateRequest;
import com.shushant.hospital_management.modules.patient.entity.Patient;
import com.shushant.hospital_management.modules.patient.mapper.PatientMapper;
import com.shushant.hospital_management.modules.patient.repository.PatientRepository;
import com.shushant.hospital_management.modules.patient.service.PatientService;
import com.shushant.hospital_management.modules.patient.validation.PatientBusinessValidator;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public PageResponse<PatientResponse> listPatients(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<PatientResponse> patientPage = patientRepository.findAllByDeletedFalse(pageable)
                .map(patientMapper::toResponse);
        return PageResponse.from(patientPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> searchPatients(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return listPatients(page, size);
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<PatientResponse> patientPage = patientRepository.searchActivePatients(query.trim(), pageable)
                .map(patientMapper::toResponse);
        return PageResponse.from(patientPage);
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
        existingPatient.setDeleted(true);
        patientRepository.save(existingPatient);
    }

    private Patient findByIdOrThrow(UUID patientId) {
        return patientRepository.findByIdAndDeletedFalse(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }
}
