package com.rian.appointment_api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 10, 0);

    @Test
    void createsAppointmentAsPendingWithoutAttempts() {
        Appointment appointment = Appointment.create("Ana Souza", "52998224725");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getAttemptCount()).isZero();
        assertThat(appointment.getProtocol()).isNull();
    }

    @Test
    void completesProcessingAppointmentWithProtocol() {
        Appointment appointment = processingAppointment();

        appointment.complete("4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getProtocol()).isEqualTo("4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d");
        assertThat(appointment.getLastError()).isNull();
        assertThat(appointment.getNextAttemptAt()).isNull();
    }

    @Test
    void rejectsCompletionWhenAppointmentIsNotProcessing() {
        Appointment appointment = Appointment.create("Ana Souza", "52998224725");

        assertThatThrownBy(() -> appointment.complete("4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnsToPendingWithBackoffAfterTransientFailure() {
        Appointment appointment = processingAppointment();

        appointment.registerFailure("Service unavailable", MAX_ATTEMPTS, INITIAL_BACKOFF, NOW);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getAttemptCount()).isEqualTo(1);
        assertThat(appointment.getLastError()).isEqualTo("Service unavailable");
        assertThat(appointment.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void doublesBackoffOnEachConsecutiveFailure() {
        Appointment appointment = processingAppointment();
        appointment.registerFailure("First failure", MAX_ATTEMPTS, INITIAL_BACKOFF, NOW);
        markAsProcessing(appointment);

        appointment.registerFailure("Second failure", MAX_ATTEMPTS, INITIAL_BACKOFF, NOW);

        assertThat(appointment.getAttemptCount()).isEqualTo(2);
        assertThat(appointment.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void failsPermanentlyWhenMaxAttemptsIsReached() {
        Appointment appointment = processingAppointment();

        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            appointment.registerFailure("Service unavailable", MAX_ATTEMPTS, INITIAL_BACKOFF, NOW);
            markAsProcessing(appointment);
        }
        appointment.registerFailure("Service unavailable", MAX_ATTEMPTS, INITIAL_BACKOFF, NOW);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.FAILED);
        assertThat(appointment.getAttemptCount()).isEqualTo(MAX_ATTEMPTS);
        assertThat(appointment.getNextAttemptAt()).isNull();
    }

    @Test
    void truncatesLongErrorMessages() {
        Appointment appointment = processingAppointment();

        appointment.registerFailure("x".repeat(800), MAX_ATTEMPTS, INITIAL_BACKOFF, NOW);

        assertThat(appointment.getLastError()).hasSize(500);
    }

    private static Appointment processingAppointment() {
        Appointment appointment = Appointment.create("Ana Souza", "52998224725");
        markAsProcessing(appointment);
        return appointment;
    }

    private static void markAsProcessing(Appointment appointment) {
        ReflectionTestUtils.setField(appointment, "status", AppointmentStatus.PROCESSING);
    }
}
