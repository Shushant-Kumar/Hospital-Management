package com.shushant.hospital_management.modules.doctor.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DoctorScheduleResponse(
        UUID doctorId,
        LocalDateTime from,
        LocalDateTime to,
        List<DoctorScheduleSlotResponse> slots) {
}
