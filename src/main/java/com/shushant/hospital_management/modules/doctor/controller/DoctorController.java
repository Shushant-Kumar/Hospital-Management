package com.shushant.hospital_management.modules.doctor.controller;

import com.shushant.hospital_management.common.dto.ApiResponse;
import com.shushant.hospital_management.common.dto.PageResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorCreateRequest;
import com.shushant.hospital_management.modules.doctor.dto.DoctorResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorScheduleResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorUpdateRequest;
import com.shushant.hospital_management.modules.doctor.service.DoctorService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping("/doctors")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DoctorResponse>> addDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        DoctorResponse response = doctorService.addDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/doctors/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.updateDoctor(doctorId, request)));
    }

    @DeleteMapping("/doctors/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteDoctor(@PathVariable UUID doctorId) {
        doctorService.deleteDoctor(doctorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/doctors/{doctorId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorScheduleResponse>> getDoctorSchedule(
            @PathVariable UUID doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorSchedule(doctorId, from, to)));
    }

    @GetMapping("/departments/{departmentId}/doctors")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    public ResponseEntity<ApiResponse<PageResponse<DoctorResponse>>> listDoctorsByDepartment(
            @PathVariable UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.listDoctorsByDepartment(departmentId, page, size)));
    }
}
