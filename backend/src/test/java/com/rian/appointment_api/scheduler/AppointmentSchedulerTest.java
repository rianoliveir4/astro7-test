package com.rian.appointment_api.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rian.appointment_api.client.ProtocolClient;
import com.rian.appointment_api.exception.ProtocolUnavailableException;
import com.rian.appointment_api.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentSchedulerTest {

    private static final Long APPOINTMENT_ID = 1L;
    private static final String GENERATED_PROTOCOL = "4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d";

    private AppointmentService appointmentService;
    private ProtocolClient protocolClient;
    private AppointmentScheduler appointmentScheduler;

    @BeforeEach
    void setUp() {
        appointmentService = mock(AppointmentService.class);
        protocolClient = mock(ProtocolClient.class);
        appointmentScheduler = new AppointmentScheduler(appointmentService, protocolClient);
    }

    @Test
    void completesAppointmentWhenProtocolIsGenerated() {
        when(protocolClient.generateProtocol()).thenReturn(GENERATED_PROTOCOL);

        appointmentScheduler.processAppointment(APPOINTMENT_ID);

        verify(appointmentService).completeAppointment(APPOINTMENT_ID, GENERATED_PROTOCOL);
        verify(appointmentService, never()).registerProcessingFailure(any(), any());
    }

    @Test
    void registersFailureWhenProtocolServiceFails() {
        when(protocolClient.generateProtocol())
                .thenThrow(new ProtocolUnavailableException("External service request failed: 500"));

        appointmentScheduler.processAppointment(APPOINTMENT_ID);

        verify(appointmentService).registerProcessingFailure(APPOINTMENT_ID, "External service request failed: 500");
    }
}
