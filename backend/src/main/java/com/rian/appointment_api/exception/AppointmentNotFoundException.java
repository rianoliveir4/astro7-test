package com.rian.appointment_api.exception;

public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(Long appointmentId) {
        super("Atendimento %d não encontrado".formatted(appointmentId));
    }
}
