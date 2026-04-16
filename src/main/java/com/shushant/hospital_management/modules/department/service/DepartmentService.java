package com.shushant.hospital_management.modules.department.service;

import com.shushant.hospital_management.modules.department.dto.DepartmentCreateRequest;
import com.shushant.hospital_management.modules.department.dto.DepartmentResponse;
import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentCreateRequest request);

    List<DepartmentResponse> listDepartments();

    DepartmentResponse getDepartmentById(UUID departmentId);
}
