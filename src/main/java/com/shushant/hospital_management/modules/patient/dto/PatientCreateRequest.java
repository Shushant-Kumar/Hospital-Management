package com.shushant.hospital_management.modules.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientCreateRequest(
        @NotBlank(message = "First name is required") @Size(max = 80, message = "First name must be at most 80 characters") String firstName,

        @NotBlank(message = "Last name is required") @Size(max = 80, message = "Last name must be at most 80 characters") String lastName,

        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 120, message = "Email must be at most 120 characters") String email,

        @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9+\\- ]{8,20}$", message = "Phone number format is invalid") String phoneNumber,

        @NotNull(message = "Date of birth is required") @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,

        @NotBlank(message = "Address is required") @Size(max = 255, message = "Address must be at most 255 characters") String address) {
}
