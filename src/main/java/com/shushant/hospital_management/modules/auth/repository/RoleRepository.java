package com.shushant.hospital_management.modules.auth.repository;

import com.shushant.hospital_management.modules.auth.entity.Role;
import com.shushant.hospital_management.modules.auth.entity.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
