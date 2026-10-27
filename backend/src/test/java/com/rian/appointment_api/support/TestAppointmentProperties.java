package com.rian.appointment_api.support;

import com.rian.appointment_api.config.AppointmentProperties;
import java.time.Duration;

public final class TestAppointmentProperties {

    public static final String PROTOCOL_BASE_URL = "https://protocol-service.test";
    public static final String PROTOCOL_PATH = "/uuid";
    public static final int MAX_ATTEMPTS = 3;
    public static final Duration INITIAL_BACKOFF = Duration.ofSeconds(5);

    private TestAppointmentProperties() {
    }

    public static AppointmentProperties create() {
        return new AppointmentProperties(
                "http://localhost:3000",
                new AppointmentProperties.Protocol(
                        PROTOCOL_BASE_URL, PROTOCOL_PATH, Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new AppointmentProperties.Processing(
                        MAX_ATTEMPTS, INITIAL_BACKOFF, 10, Duration.ofMinutes(2)),
                new AppointmentProperties.Job(Duration.ofSeconds(1)));
    }
}
