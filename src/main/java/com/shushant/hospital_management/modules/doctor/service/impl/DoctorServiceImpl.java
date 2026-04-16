package com.shushant.hospital_management.modules.doctor.service.impl;

import com.shushant.hospital_management.common.dto.PageResponse;
import com.shushant.hospital_management.common.exception.BusinessException;
import com.shushant.hospital_management.common.exception.ErrorCode;
import com.shushant.hospital_management.common.exception.ResourceConflictException;
import com.shushant.hospital_management.common.exception.ResourceNotFoundException;
import com.shushant.hospital_management.modules.appointment.entity.Appointment;
import com.shushant.hospital_management.modules.appointment.entity.AppointmentStatus;
import com.shushant.hospital_management.modules.appointment.repository.AppointmentRepository;
import com.shushant.hospital_management.modules.department.entity.Department;
import com.shushant.hospital_management.modules.department.repository.DepartmentRepository;
import com.shushant.hospital_management.modules.doctor.dto.DoctorCreateRequest;
import com.shushant.hospital_management.modules.doctor.dto.DoctorResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorScheduleResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorScheduleSlotResponse;
import com.shushant.hospital_management.modules.doctor.dto.DoctorUpdateRequest;
import com.shushant.hospital_management.modules.doctor.entity.Doctor;
import com.shushant.hospital_management.modules.doctor.repository.DoctorRepository;
import com.shushant.hospital_management.modules.doctor.service.DoctorService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private static final Set<AppointmentStatus> SCHEDULE_STATUSES = Set.of(AppointmentStatus.BOOKED,
            AppointmentStatus.RESCHEDULED);

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorServiceImpl(
            DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public DoctorResponse addDoctor(DoctorCreateRequest request) {
        validateUniquenessForCreate(request.email(), request.licenseNumber(), request.phoneNumber());
        Department department = findDepartmentOrThrow(request.departmentId());

        Doctor doctor = new Doctor();
        applyCommonFields(doctor, request.firstName(), request.lastName(), request.email(), request.phoneNumber(),
                request.specialization(), request.licenseNumber(), request.consultationDurationMinutes(), department);

        Doctor savedDoctor = doctorRepository.save(doctor);
        return toResponse(savedDoctor);
    }

    @Override
    public DoctorResponse updateDoctor(UUID doctorId, DoctorUpdateRequest request) {
        Doctor doctor = findDoctorOrThrow(doctorId);
        validateUniquenessForUpdate(doctorId, request.email(), request.licenseNumber(), request.phoneNumber());
        Department department = findDepartmentOrThrow(request.departmentId());

        applyCommonFields(doctor, request.firstName(), request.lastName(), request.email(), request.phoneNumber(),
                request.specialization(), request.licenseNumber(), request.consultationDurationMinutes(), department);
        doctor.setActive(request.active());

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return toResponse(updatedDoctor);
    }

    @Override
    public void deleteDoctor(UUID doctorId) {
        Doctor doctor = findDoctorOrThrow(doctorId);
        doctor.setDeleted(true);
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorScheduleResponse getDoctorSchedule(UUID doctorId, LocalDateTime from, LocalDateTime to) {
        Doctor doctor = findDoctorOrThrow(doctorId);

        LocalDateTime start = from == null ? LocalDateTime.now() : from;
        LocalDateTime end = to == null ? start.plusDays(7) : to;
        if (!end.isAfter(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Schedule range is invalid. 'to' must be after 'from'");
        }

        List<Appointment> appointments = appointmentRepository
                .findAllByDoctorIdAndDeletedFalseAndStatusInAndStartTimeBetweenOrderByStartTimeAsc(
                        doctorId,
                        SCHEDULE_STATUSES,
                        start,
                        end);

        List<DoctorScheduleSlotResponse> slots = appointments.stream()
                .map(this::toSlot)
                .toList();

        return new DoctorScheduleResponse(doctor.getId(), start, end, slots);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> listDoctorsByDepartment(UUID departmentId, int page, int size) {
        findDepartmentOrThrow(departmentId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<DoctorResponse> doctorsPage = doctorRepository
                .findAllByDepartmentIdAndDeletedFalseAndActiveTrue(departmentId, pageable)
                .map(this::toResponse);
        return PageResponse.from(doctorsPage);
    }

    private Doctor findDoctorOrThrow(UUID doctorId) {
        return doctorRepository.findByIdAndDeletedFalse(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
    }

    private Department findDepartmentOrThrow(UUID departmentId) {
        return departmentRepository.findByIdAndDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
    }

    private void validateUniquenessForCreate(String email, String licenseNumber, String phoneNumber) {
        if (doctorRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            throw new ResourceConflictException("A doctor with this email already exists");
        }
        if (doctorRepository.existsByLicenseNumberIgnoreCaseAndDeletedFalse(licenseNumber)) {
            throw new ResourceConflictException("A doctor with this license number already exists");
        }
        if (doctorRepository.existsByPhoneNumberAndDeletedFalse(phoneNumber)) {
            throw new ResourceConflictException("A doctor with this phone number already exists");
        }
    }

    private void validateUniquenessForUpdate(UUID doctorId, String email, String licenseNumber, String phoneNumber) {
        if (doctorRepository.existsByEmailIgnoreCaseAndIdNotAndDeletedFalse(email, doctorId)) {
            throw new ResourceConflictException("A doctor with this email already exists");
        }
        if (doctorRepository.existsByLicenseNumberIgnoreCaseAndIdNotAndDeletedFalse(licenseNumber, doctorId)) {
            throw new ResourceConflictException("A doctor with this license number already exists");
        }
        if (doctorRepository.existsByPhoneNumberAndIdNotAndDeletedFalse(phoneNumber, doctorId)) {
            throw new ResourceConflictException("A doctor with this phone number already exists");
        }
    }

    private void applyCommonFields(
            Doctor doctor,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String specialization,
            String licenseNumber,
            int consultationDurationMinutes,
            Department department) {
        doctor.setFirstName(firstName.trim());
        doctor.setLastName(lastName.trim());
        doctor.setEmail(email.trim().toLowerCase());
        doctor.setPhoneNumber(phoneNumber.trim());
        doctor.setSpecialization(specialization.trim());
        doctor.setLicenseNumber(licenseNumber.trim().toUpperCase());
        doctor.setConsultationDurationMinutes(consultationDurationMinutes <= 0 ? 30 : consultationDurationMinutes);
        doctor.setDepartment(department);
    }

    private DoctorScheduleSlotResponse toSlot(Appointment appointment) {
        String patientName = appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName();
        return new DoctorScheduleSlotResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                patientName,
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus());
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getEmail(),
                doctor.getPhoneNumber(),
                doctor.getSpecialization(),
                doctor.getLicenseNumber(),
                doctor.getConsultationDurationMinutes(),
                doctor.isActive(),
                doctor.getDepartment().getId(),
                doctor.getDepartment().getName(),
                doctor.getDepartment().getCode(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt());
    }
}
