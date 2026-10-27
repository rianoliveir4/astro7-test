package com.rian.appointment_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

public record CreateAppointmentRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String patientName,

        @NotBlank(message = "O CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String patientCpf) {
}
