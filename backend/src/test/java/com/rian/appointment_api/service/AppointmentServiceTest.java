package com.rian.appointment_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rian.appointment_api.dto.AppointmentResponse;
import com.rian.appointment_api.dto.CreateAppointmentRequest;
import com.rian.appointment_api.entity.Appointment;
import com.rian.appointment_api.entity.AppointmentStatus;
import com.rian.appointment_api.exception.ActiveAppointmentAlreadyExistsException;
import com.rian.appointment_api.exception.AppointmentNotFoundException;
import com.rian.appointment_api.repository.AppointmentRepository;
import com.rian.appointment_api.support.TestAppointmentProperties;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentServiceTest {

    private static final Long APPOINTMENT_ID = 1L;
    private static final String GENERATED_PROTOCOL = "4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d";

    private AppointmentRepository appointmentRepository;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        appointmentService = new AppointmentService(appointmentRepository, TestAppointmentProperties.create());
    }

    @Test
    void createsPendingAppointmentWithNormalizedCpf() {
        when(appointmentRepository.findFirstByPatientCpfAndStatusIn(eq("52998224725"), anyCollection()))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse createdAppointment = appointmentService.createAppointment(
                new CreateAppointmentRequest("  Ana Souza ", "529.982.247-25"));

        assertThat(createdAppointment.patientName()).isEqualTo("Ana Souza");
        assertThat(createdAppointment.patientCpf()).isEqualTo("52998224725");
        assertThat(createdAppointment.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void rejectsNewAppointmentWhenPatientHasActiveOne() {
        Appointment activeAppointment = appointmentWithStatus(AppointmentStatus.PENDING);
        when(appointmentRepository.findFirstByPatientCpfAndStatusIn(eq("52998224725"), anyCollection()))
                .thenReturn(Optional.of(activeAppointment));

        assertThatThrownBy(() -> appointmentService.createAppointment(
                new CreateAppointmentRequest("Ana Souza", "529.982.247-25")))
                .isInstanceOf(ActiveAppointmentAlreadyExistsException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundForUnknownAppointment() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.findAppointmentById(99L))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void completesProcessingAppointment() {
        Appointment processingAppointment = appointmentWithStatus(AppointmentStatus.PROCESSING);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(processingAppointment));

        appointmentService.completeAppointment(APPOINTMENT_ID, GENERATED_PROTOCOL);

        assertThat(processingAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(processingAppointment.getProtocol()).isEqualTo(GENERATED_PROTOCOL);
    }

    @Test
    void keepsAppointmentForRetryAfterProcessingFailure() {
        Appointment processingAppointment = appointmentWithStatus(AppointmentStatus.PROCESSING);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(processingAppointment));

        appointmentService.registerProcessingFailure(APPOINTMENT_ID, "External service request failed: 500");

        assertThat(processingAppointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(processingAppointment.getAttemptCount()).isEqualTo(1);
        assertThat(processingAppointment.getLastError()).isEqualTo("External service request failed: 500");
        assertThat(processingAppointment.getNextAttemptAt()).isNotNull();
    }

    @Test
    void marksAppointmentAsFailedAfterMaxAttempts() {
        Appointment processingAppointment = appointmentWithStatus(AppointmentStatus.PROCESSING);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(processingAppointment));

        for (int attempt = 1; attempt <= TestAppointmentProperties.MAX_ATTEMPTS; attempt++) {
            ReflectionTestUtils.setField(processingAppointment, "status", AppointmentStatus.PROCESSING);
            appointmentService.registerProcessingFailure(APPOINTMENT_ID, "timeout");
        }

        assertThat(processingAppointment.getStatus()).isEqualTo(AppointmentStatus.FAILED);
        assertThat(processingAppointment.getAttemptCount()).isEqualTo(TestAppointmentProperties.MAX_ATTEMPTS);
    }

    private static Appointment appointmentWithStatus(AppointmentStatus status) {
        Appointment appointment = Appointment.create("Ana Souza", "52998224725");
        ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
        ReflectionTestUtils.setField(appointment, "status", status);
        return appointment;
    }
}
