package com.shushant.hospital_management.modules.doctor.repository;

import com.shushant.hospital_management.modules.doctor.entity.Doctor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Optional<Doctor> findByIdAndDeletedFalse(UUID doctorId);

    Page<Doctor> findAllByDepartmentIdAndDeletedFalseAndActiveTrue(UUID departmentId, Pageable pageable);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByLicenseNumberIgnoreCaseAndDeletedFalse(String licenseNumber);

    boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

    boolean existsByEmailIgnoreCaseAndIdNotAndDeletedFalse(String email, UUID doctorId);

    boolean existsByLicenseNumberIgnoreCaseAndIdNotAndDeletedFalse(String licenseNumber, UUID doctorId);

    boolean existsByPhoneNumberAndIdNotAndDeletedFalse(String phoneNumber, UUID doctorId);
}
