package com.shushant.hospital_management.modules.appointment.repository;

import com.shushant.hospital_management.modules.appointment.entity.Appointment;
import com.shushant.hospital_management.modules.appointment.entity.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findAllByDoctorIdAndDeletedFalseAndStatusInAndStartTimeBetweenOrderByStartTimeAsc(
            UUID doctorId,
            Collection<AppointmentStatus> statuses,
            LocalDateTime from,
            LocalDateTime to);
}
