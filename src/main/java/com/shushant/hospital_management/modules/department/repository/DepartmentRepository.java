package com.shushant.hospital_management.modules.department.repository;

import com.shushant.hospital_management.modules.department.entity.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByIdAndDeletedFalse(UUID departmentId);

    List<Department> findAllByDeletedFalseOrderByNameAsc();

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
}
