package com.shushant.hospital_management.modules.patient.service;

import com.shushant.hospital_management.modules.patient.dto.PatientCreateRequest;
import com.shushant.hospital_management.modules.patient.dto.PatientResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientUpdateRequest;
import com.shushant.hospital_management.common.dto.PageResponse;
import java.util.UUID;

public interface PatientService {

    PatientResponse createPatient(PatientCreateRequest request);

    PatientResponse getPatientById(UUID patientId);

    PageResponse<PatientResponse> listPatients(int page, int size);

    PageResponse<PatientResponse> searchPatients(String query, int page, int size);

    PatientResponse updatePatient(UUID patientId, PatientUpdateRequest request);

    void deletePatient(UUID patientId);
}
