package com.rian.appointment_api.service;

import com.rian.appointment_api.config.AppointmentProperties;
import com.rian.appointment_api.dto.AppointmentResponse;
import com.rian.appointment_api.dto.CreateAppointmentRequest;
import com.rian.appointment_api.entity.Appointment;
import com.rian.appointment_api.entity.AppointmentStatus;
import com.rian.appointment_api.exception.ActiveAppointmentAlreadyExistsException;
import com.rian.appointment_api.exception.AppointmentNotFoundException;
import com.rian.appointment_api.repository.AppointmentRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.PROCESSING);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentProperties appointmentProperties;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest createAppointmentRequest) {
        String normalizedCpf = normalizeCpf(createAppointmentRequest.patientCpf());

        appointmentRepository.findFirstByPatientCpfAndStatusIn(normalizedCpf, ACTIVE_STATUSES)
                .ifPresent(activeAppointment -> {
                    throw new ActiveAppointmentAlreadyExistsException(activeAppointment.getId());
                });

        Appointment newAppointment = Appointment.create(
                createAppointmentRequest.patientName().trim(),
                normalizedCpf);

        return AppointmentResponse.from(appointmentRepository.save(newAppointment));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findAppointmentById(Long appointmentId) {
        return AppointmentResponse.from(getAppointment(appointmentId));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAppointmentsByIds(Collection<Long> appointmentIds) {
        return appointmentRepository.findAllById(appointmentIds).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional
    public List<Long> claimAppointmentsReadyForProcessing() {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = appointmentProperties.processing().batchSize();

        return appointmentRepository.findIdsReadyForProcessing(now, Limit.of(batchSize)).stream()
                .filter(appointmentId -> appointmentRepository.claimForProcessing(appointmentId, now) == 1)
                .toList();
    }

    @Transactional
    public int releaseStuckAppointments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime stuckSince = now.minus(appointmentProperties.processing().stuckTimeout());
        return appointmentRepository.releaseStuckAppointments(stuckSince, now);
    }

    @Transactional
    public void completeAppointment(Long appointmentId, String generatedProtocol) {
        getAppointment(appointmentId).complete(generatedProtocol);
    }

    @Transactional
    public void registerProcessingFailure(Long appointmentId, String errorMessage) {
        AppointmentProperties.Processing processingProperties = appointmentProperties.processing();
        getAppointment(appointmentId).registerFailure(
                errorMessage,
                processingProperties.maxAttempts(),
                processingProperties.initialBackoff(),
                LocalDateTime.now());
    }

    private Appointment getAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    private static String normalizeCpf(String patientCpf) {
        return patientCpf.replaceAll("\\D", "");
    }
}
