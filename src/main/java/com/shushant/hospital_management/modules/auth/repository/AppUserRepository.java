package com.shushant.hospital_management.modules.auth.repository;

import com.shushant.hospital_management.modules.auth.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCase(String email);
}
