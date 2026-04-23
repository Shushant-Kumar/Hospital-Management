package com.shushant.hospital_management.util;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized Role-Based Access Control (RBAC) manager.
 * All permission checks across the application should go through this class.
 */
public final class RBACManager {

    private RBACManager() {}

    // ── Modules ──────────────────────────────────────────────────────────────

    public enum Module {
        DASHBOARD("Dashboard"),
        PATIENTS("Patients"),
        DOCTORS("Doctors"),
        APPOINTMENTS("Appointments"),
        BILLING("Billing"),
        PHARMACY("Pharmacy"),
        LAB_TESTS("Lab Tests"),
        BEDS("Beds & Wards"),
        USERS("Users"),
        PATIENT_PORTAL("My Dashboard");

        private final String displayName;
        Module(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    public enum Permission {
        VIEW,
        CREATE,
        EDIT,
        DELETE,
        // Module-specific permissions
        RECORD_PAYMENT,
        ENTER_LAB_RESULT,
        COLLECT_SAMPLE,
        PROCESS_LAB,
        ASSIGN_BED,
        RELEASE_BED,
        CHECKIN_APPOINTMENT,
        COMPLETE_APPOINTMENT,
        CANCEL_APPOINTMENT,
        BOOK_OWN_APPOINTMENT,
        CANCEL_OWN_APPOINTMENT,
        VIEW_OWN_DATA
    }

    // ── Permission Matrix ────────────────────────────────────────────────────

    private static final Map<String, Map<Module, Set<Permission>>> ROLE_PERMISSIONS = Map.ofEntries(

        Map.entry("ADMIN", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.PATIENTS,     Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT, Permission.DELETE)),
            Map.entry(Module.DOCTORS,      Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT, Permission.DELETE)),
            Map.entry(Module.APPOINTMENTS, Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT, Permission.DELETE,
                    Permission.CHECKIN_APPOINTMENT, Permission.COMPLETE_APPOINTMENT, Permission.CANCEL_APPOINTMENT)),
            Map.entry(Module.BILLING,      Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT, Permission.RECORD_PAYMENT)),
            Map.entry(Module.PHARMACY,     Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT)),
            Map.entry(Module.LAB_TESTS,    Set.of(Permission.VIEW, Permission.CREATE, Permission.ENTER_LAB_RESULT,
                    Permission.COLLECT_SAMPLE, Permission.PROCESS_LAB)),
            Map.entry(Module.BEDS,         Set.of(Permission.VIEW, Permission.CREATE, Permission.ASSIGN_BED, Permission.RELEASE_BED)),
            Map.entry(Module.USERS,        Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT, Permission.DELETE))
        )),

        Map.entry("DOCTOR", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.PATIENTS,     Set.of(Permission.VIEW)),
            Map.entry(Module.APPOINTMENTS, Set.of(Permission.VIEW, Permission.CREATE,
                    Permission.CHECKIN_APPOINTMENT, Permission.COMPLETE_APPOINTMENT, Permission.CANCEL_APPOINTMENT)),
            Map.entry(Module.LAB_TESTS,    Set.of(Permission.VIEW, Permission.CREATE))
        )),

        Map.entry("NURSE", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.PATIENTS,     Set.of(Permission.VIEW)),
            Map.entry(Module.APPOINTMENTS, Set.of(Permission.VIEW)),
            Map.entry(Module.BEDS,         Set.of(Permission.VIEW, Permission.ASSIGN_BED, Permission.RELEASE_BED))
        )),

        Map.entry("RECEPTIONIST", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.PATIENTS,     Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT)),
            Map.entry(Module.DOCTORS,      Set.of(Permission.VIEW)),
            Map.entry(Module.APPOINTMENTS, Set.of(Permission.VIEW, Permission.CREATE,
                    Permission.CHECKIN_APPOINTMENT, Permission.CANCEL_APPOINTMENT)),
            Map.entry(Module.BILLING,      Set.of(Permission.VIEW, Permission.CREATE)),
            Map.entry(Module.BEDS,         Set.of(Permission.VIEW, Permission.ASSIGN_BED, Permission.RELEASE_BED))
        )),

        Map.entry("PHARMACIST", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.PHARMACY,     Set.of(Permission.VIEW, Permission.CREATE, Permission.EDIT))
        )),

        Map.entry("LAB_TECHNICIAN", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.LAB_TESTS,    Set.of(Permission.VIEW, Permission.ENTER_LAB_RESULT,
                    Permission.COLLECT_SAMPLE, Permission.PROCESS_LAB))
        )),

        Map.entry("ACCOUNTANT", Map.ofEntries(
            Map.entry(Module.DASHBOARD,    Set.of(Permission.VIEW)),
            Map.entry(Module.BILLING,      Set.of(Permission.VIEW, Permission.CREATE, Permission.RECORD_PAYMENT))
        )),

        Map.entry("PATIENT", Map.ofEntries(
            Map.entry(Module.PATIENT_PORTAL, Set.of(Permission.VIEW, Permission.BOOK_OWN_APPOINTMENT,
                    Permission.CANCEL_OWN_APPOINTMENT, Permission.VIEW_OWN_DATA))
        ))
    );

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Check if the current user has a specific permission on a module.
     */
    public static boolean hasPermission(Module module, Permission permission) {
        String role = SessionManager.getCurrentRole();
        if (role == null) return false;
        Map<Module, Set<Permission>> rolePerms = ROLE_PERMISSIONS.get(role.toUpperCase());
        if (rolePerms == null) return false;
        Set<Permission> modulePerms = rolePerms.get(module);
        if (modulePerms == null) return false;
        return modulePerms.contains(permission);
    }

    /**
     * Guard method — checks permission, shows access denied dialog if not permitted.
     * Returns true if permission is granted.
     */
    public static boolean requirePermission(Module module, Permission permission, Component parent) {
        if (hasPermission(module, permission)) return true;
        JOptionPane.showMessageDialog(parent,
                "Access Denied.\n\nYou do not have permission to perform this action.\n" +
                "Required: " + module.getDisplayName() + " → " + permission.name() + "\n" +
                "Your role: " + SessionManager.getCurrentRole(),
                "Access Denied", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * Returns the list of modules the current user can access (has VIEW permission for).
     */
    public static List<Module> getAccessibleModules() {
        String role = SessionManager.getCurrentRole();
        if (role == null) return List.of();
        Map<Module, Set<Permission>> rolePerms = ROLE_PERMISSIONS.get(role.toUpperCase());
        if (rolePerms == null) return List.of();
        List<Module> modules = new ArrayList<>();
        for (Module m : Module.values()) {
            Set<Permission> perms = rolePerms.get(m);
            if (perms != null && perms.contains(Permission.VIEW)) {
                modules.add(m);
            }
        }
        return modules;
    }

    /**
     * Check if the current user can view a specific module.
     */
    public static boolean canView(Module module) {
        return hasPermission(module, Permission.VIEW);
    }

    /**
     * Check if the current user is a PATIENT role.
     */
    public static boolean isPatientRole() {
        return SessionManager.hasRole("PATIENT");
    }

    /**
     * Check if the current user is a DOCTOR role.
     */
    public static boolean isDoctorRole() {
        return SessionManager.hasRole("DOCTOR");
    }
}
