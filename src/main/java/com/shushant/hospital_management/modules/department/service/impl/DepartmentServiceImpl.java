package com.shushant.hospital_management.modules.department.service.impl;

import com.shushant.hospital_management.common.exception.ResourceConflictException;
import com.shushant.hospital_management.common.exception.ResourceNotFoundException;
import com.shushant.hospital_management.modules.department.dto.DepartmentCreateRequest;
import com.shushant.hospital_management.modules.department.dto.DepartmentResponse;
import com.shushant.hospital_management.modules.department.entity.Department;
import com.shushant.hospital_management.modules.department.repository.DepartmentRepository;
import com.shushant.hospital_management.modules.department.service.DepartmentService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        if (departmentRepository.existsByNameIgnoreCaseAndDeletedFalse(request.name())) {
            throw new ResourceConflictException("A department with this name already exists");
        }
        if (departmentRepository.existsByCodeIgnoreCaseAndDeletedFalse(request.code())) {
            throw new ResourceConflictException("A department with this code already exists");
        }

        Department department = new Department();
        department.setName(request.name().trim());
        department.setCode(request.code().trim().toUpperCase());
        department.setDescription(request.description() == null ? null : request.description().trim());

        Department savedDepartment = departmentRepository.save(department);
        return toResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAllByDeletedFalseOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(UUID departmentId) {
        Department department = findDepartmentOrThrow(departmentId);
        return toResponse(department);
    }

    private Department findDepartmentOrThrow(UUID departmentId) {
        return departmentRepository.findByIdAndDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.isActive(),
                department.getCreatedAt(),
                department.getUpdatedAt());
    }
}
