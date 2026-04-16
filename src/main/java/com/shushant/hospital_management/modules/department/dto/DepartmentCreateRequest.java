package com.shushant.hospital_management.modules.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String code,
        @Size(max = 255) String description) {
}
