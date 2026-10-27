package com.rian.appointment_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rian.appointment_api.dto.AppointmentResponse;
import com.rian.appointment_api.dto.CreateAppointmentRequest;
import com.rian.appointment_api.entity.AppointmentStatus;
import com.rian.appointment_api.exception.ActiveAppointmentAlreadyExistsException;
import com.rian.appointment_api.exception.AppointmentNotFoundException;
import com.rian.appointment_api.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    private static final String VALID_CPF = "529.982.247-25";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    void createsAppointmentAndReturnsLocation() throws Exception {
        when(appointmentService.createAppointment(any(CreateAppointmentRequest.class)))
                .thenReturn(pendingAppointmentResponse(1L));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientName": "Ana Souza", "patientCpf": "%s"}
                                """.formatted(VALID_CPF)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/appointments/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsInvalidCpfAndBlankName() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientName": "", "patientCpf": "111.222.333-44"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.patientCpf").value("CPF inválido"))
                .andExpect(jsonPath("$.errors.patientName").exists());

        verify(appointmentService, never()).createAppointment(any());
    }

    @Test
    void returnsConflictWhenPatientHasActiveAppointment() throws Exception {
        when(appointmentService.createAppointment(any(CreateAppointmentRequest.class)))
                .thenThrow(new ActiveAppointmentAlreadyExistsException(7L));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientName": "Ana Souza", "patientCpf": "%s"}
                                """.formatted(VALID_CPF)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.activeAppointmentId").value(7));
    }

    @Test
    void returnsAppointmentById() throws Exception {
        when(appointmentService.findAppointmentById(1L)).thenReturn(pendingAppointmentResponse(1L));

        mockMvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Ana Souza"));
    }

    @Test
    void returnsNotFoundForUnknownAppointment() throws Exception {
        when(appointmentService.findAppointmentById(99L)).thenThrow(new AppointmentNotFoundException(99L));

        mockMvc.perform(get("/api/appointments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Atendimento não encontrado"));
    }

    @Test
    void returnsAppointmentsByIds() throws Exception {
        when(appointmentService.findAppointmentsByIds(List.of(1L, 2L)))
                .thenReturn(List.of(pendingAppointmentResponse(1L), pendingAppointmentResponse(2L)));

        mockMvc.perform(get("/api/appointments").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private static AppointmentResponse pendingAppointmentResponse(Long appointmentId) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 26, 10, 0);
        return new AppointmentResponse(
                appointmentId, "Ana Souza", "52998224725", AppointmentStatus.PENDING,
                null, 0, null, createdAt, createdAt);
    }
}
