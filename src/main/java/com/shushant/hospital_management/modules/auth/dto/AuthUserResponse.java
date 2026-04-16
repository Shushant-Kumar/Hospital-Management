package com.shushant.hospital_management.modules.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles) {
}
