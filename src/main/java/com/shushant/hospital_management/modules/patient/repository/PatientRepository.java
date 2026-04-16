package com.shushant.hospital_management.modules.patient.repository;

import com.shushant.hospital_management.modules.patient.entity.Patient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndDeletedFalse(UUID patientId);

    Page<Patient> findAllByDeletedFalse(Pageable pageable);

    @Query("""
            select p from Patient p
            where p.deleted = false and (
                lower(p.firstName) like lower(concat('%', :query, '%'))
                or lower(p.lastName) like lower(concat('%', :query, '%'))
                or lower(p.email) like lower(concat('%', :query, '%'))
                or p.phoneNumber like concat('%', :query, '%')
            )
            """)
    Page<Patient> searchActivePatients(@Param("query") String query, Pageable pageable);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

    boolean existsByEmailIgnoreCaseAndIdNotAndDeletedFalse(String email, UUID id);

    boolean existsByPhoneNumberAndIdNotAndDeletedFalse(String phoneNumber, UUID id);
}
