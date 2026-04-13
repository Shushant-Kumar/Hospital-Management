package com.shushant.hospital_management.modules.patient.service;

import com.shushant.hospital_management.modules.patient.dto.PatientCreateRequest;
import com.shushant.hospital_management.modules.patient.dto.PatientResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface PatientService {

    PatientResponse createPatient(PatientCreateRequest request);

    PatientResponse getPatientById(UUID patientId);

    List<PatientResponse> getAllPatients();

    PatientResponse updatePatient(UUID patientId, PatientUpdateRequest request);

    void deletePatient(UUID patientId);
}
