package com.rian.appointment_api.exception;

import lombok.Getter;

@Getter
public class ActiveAppointmentAlreadyExistsException extends RuntimeException {

    private final Long activeAppointmentId;

    public ActiveAppointmentAlreadyExistsException(Long activeAppointmentId) {
        super("Já existe um atendimento em andamento para este CPF");
        this.activeAppointmentId = activeAppointmentId;
    }
}
