package com.rian.appointment_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("appointment")
public record AppointmentProperties(
        @NotBlank String frontendUrl,
        @Valid @NotNull Protocol protocol,
        @Valid @NotNull Processing processing,
        @Valid @NotNull Job job) {

    public record Protocol(
            @NotBlank String baseUrl,
            @NotBlank String path,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout) {
    }

    public record Processing(
            @Positive int maxAttempts,
            @NotNull Duration initialBackoff,
            @Positive int batchSize,
            @NotNull Duration stuckTimeout) {
    }

    public record Job(@NotNull Duration interval) {
    }
}
