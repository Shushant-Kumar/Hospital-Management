package com.shushant.hospital_management.modules.doctor.service;

import com.shushant.hospital_management.common.dto.PageResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorCreateRequest;
import com.shushant.hospital_management.modules.doctor.dto.DoctorResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorScheduleResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorUpdateRequest;
import java.time.LocalDateTime;
import java.util.UUID;

public interface DoctorService {

    DoctorResponse addDoctor(DoctorCreateRequest request);

    DoctorResponse updateDoctor(UUID doctorId, DoctorUpdateRequest request);

    void deleteDoctor(UUID doctorId);

    DoctorScheduleResponse getDoctorSchedule(UUID doctorId, LocalDateTime from, LocalDateTime to);

    PageResponse<DoctorResponse> listDoctorsByDepartment(UUID departmentId, int page, int size);
}
