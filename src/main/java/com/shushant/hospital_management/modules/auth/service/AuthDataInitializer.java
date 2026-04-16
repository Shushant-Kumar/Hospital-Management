package com.shushant.hospital_management.modules.auth.service;

import com.shushant.hospital_management.modules.auth.entity.Permission;
import com.shushant.hospital_management.modules.auth.entity.Role;
import com.shushant.hospital_management.modules.auth.entity.RoleName;
import com.shushant.hospital_management.modules.auth.repository.PermissionRepository;
import com.shushant.hospital_management.modules.auth.repository.RoleRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public AuthDataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Permission patientRead = ensurePermission("PATIENT_READ", "Read patient records");
        Permission patientWrite = ensurePermission("PATIENT_WRITE", "Create or update patient records");

        ensureRole(RoleName.ROLE_ADMIN, "System administrator", Set.of(patientRead, patientWrite));
        ensureRole(RoleName.ROLE_DOCTOR, "Doctor account", Set.of(patientRead));
        ensureRole(RoleName.ROLE_STAFF, "Staff account", Set.of(patientRead, patientWrite));
        ensureRole(RoleName.ROLE_BILLING, "Billing account", Set.of());
        ensureRole(RoleName.ROLE_PATIENT, "Patient self-service account", Set.of());
    }

    private Permission ensurePermission(String name, String description) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            Permission permission = new Permission();
            permission.setName(name);
            permission.setDescription(description);
            return permissionRepository.save(permission);
        });
    }

    private void ensureRole(RoleName roleName, String description, Set<Permission> permissions) {
        roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(description);
            role.setPermissions(permissions);
            return roleRepository.save(role);
        });
    }
}
