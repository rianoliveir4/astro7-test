package com.rian.appointment_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_BACKOFF_EXPONENT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String patientName;

    @Column(nullable = false, length = 11)
    private String patientCpf;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(length = 36, unique = true)
    private String protocol;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = MAX_ERROR_MESSAGE_LENGTH)
    private String lastError;

    private LocalDateTime nextAttemptAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Appointment create(String patientName, String patientCpf) {
        Appointment appointment = new Appointment();
        appointment.patientName = patientName;
        appointment.patientCpf = patientCpf;
        appointment.status = AppointmentStatus.PENDING;
        appointment.attemptCount = 0;
        return appointment;
    }

    public void complete(String generatedProtocol) {
        ensureIsProcessing();
        this.protocol = generatedProtocol;
        this.status = AppointmentStatus.COMPLETED;
        this.lastError = null;
        this.nextAttemptAt = null;
    }

    public void registerFailure(String errorMessage, int maxAttempts, Duration initialBackoff, LocalDateTime now) {
        ensureIsProcessing();
        this.attemptCount++;
        this.lastError = truncateErrorMessage(errorMessage);

        if (attemptCount >= maxAttempts) {
            this.status = AppointmentStatus.FAILED;
            this.nextAttemptAt = null;
            return;
        }

        this.status = AppointmentStatus.PENDING;
        this.nextAttemptAt = now.plus(calculateBackoff(initialBackoff));
    }

    private Duration calculateBackoff(Duration initialBackoff) {
        int backoffExponent = Math.min(attemptCount - 1, MAX_BACKOFF_EXPONENT);
        return initialBackoff.multipliedBy(1L << backoffExponent);
    }

    private void ensureIsProcessing() {
        if (status != AppointmentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Appointment %d must be PROCESSING but was %s".formatted(id, status));
        }
    }

    private static String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
