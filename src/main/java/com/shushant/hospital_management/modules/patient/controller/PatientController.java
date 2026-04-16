package com.shushant.hospital_management.modules.patient.controller;

import com.shushant.hospital_management.common.dto.ApiResponse;
import com.shushant.hospital_management.common.dto.PageResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientCreateRequest;
import com.shushant.hospital_management.modules.patient.dto.PatientResponse;
import com.shushant.hospital_management.modules.patient.dto.PatientUpdateRequest;
import com.shushant.hospital_management.modules.patient.service.PatientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody PatientCreateRequest request) {
        PatientResponse patientResponse = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(patientResponse));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable UUID patientId) {
        PatientResponse patientResponse = patientService.getPatientById(patientId);
        return ResponseEntity.ok(ApiResponse.success(patientResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> listPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(patientService.listPatients(page, size)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> searchPatients(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(patientService.searchPatients(query, page, size)));
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientUpdateRequest request) {
        PatientResponse patientResponse = patientService.updatePatient(patientId, request);
        return ResponseEntity.ok(ApiResponse.success(patientResponse));
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID patientId) {
        patientService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }
}
