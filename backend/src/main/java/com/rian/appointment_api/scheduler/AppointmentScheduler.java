package com.rian.appointment_api.scheduler;

import com.rian.appointment_api.client.ProtocolClient;
import com.rian.appointment_api.service.AppointmentService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final AppointmentService appointmentService;
    private final ProtocolClient protocolClient;

    @Scheduled(fixedDelayString = "${appointment.job.interval}")
    public void processPendingAppointments() {
        int releasedAppointments = appointmentService.releaseStuckAppointments();
        if (releasedAppointments > 0) {
            log.warn("Released {} stuck appointments back to PENDING", releasedAppointments);
        }

        appointmentService.claimAppointmentsReadyForProcessing()
                .forEach(appointmentId -> virtualThreadExecutor.execute(() -> processAppointment(appointmentId)));
    }

    void processAppointment(Long appointmentId) {
        try {
            String generatedProtocol = protocolClient.generateProtocol();
            appointmentService.completeAppointment(appointmentId, generatedProtocol);
            log.info("Appointment {} completed with protocol {}", appointmentId, generatedProtocol);
        } catch (Exception processingException) {
            log.warn("Failed to process appointment {}: {}", appointmentId, processingException.getMessage());
            appointmentService.registerProcessingFailure(appointmentId, processingException.getMessage());
        }
    }
}
