package com.rian.appointment_api.dto;

import com.rian.appointment_api.entity.Appointment;
import com.rian.appointment_api.entity.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        String patientName,
        String patientCpf,
        AppointmentStatus status,
        String protocol,
        int attemptCount,
        LocalDateTime nextAttemptAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientName(),
                appointment.getPatientCpf(),
                appointment.getStatus(),
                appointment.getProtocol(),
                appointment.getAttemptCount(),
                appointment.getNextAttemptAt(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt());
    }
}
