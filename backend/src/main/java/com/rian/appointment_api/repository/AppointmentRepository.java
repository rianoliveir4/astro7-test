package com.rian.appointment_api.repository;

import com.rian.appointment_api.entity.Appointment;
import com.rian.appointment_api.entity.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findFirstByPatientCpfAndStatusIn(String patientCpf, Collection<AppointmentStatus> statuses);

    @Query("""
            select appointment.id from Appointment appointment
            where appointment.status = com.rian.appointment_api.entity.AppointmentStatus.PENDING
              and (appointment.nextAttemptAt is null or appointment.nextAttemptAt <= :now)
            order by appointment.createdAt
            """)
    List<Long> findIdsReadyForProcessing(@Param("now") LocalDateTime now, Limit limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Appointment appointment
            set appointment.status = com.rian.appointment_api.entity.AppointmentStatus.PROCESSING,
                appointment.updatedAt = :now
            where appointment.id = :appointmentId
              and appointment.status = com.rian.appointment_api.entity.AppointmentStatus.PENDING
            """)
    int claimForProcessing(@Param("appointmentId") Long appointmentId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Appointment appointment
            set appointment.status = com.rian.appointment_api.entity.AppointmentStatus.PENDING,
                appointment.updatedAt = :now
            where appointment.status = com.rian.appointment_api.entity.AppointmentStatus.PROCESSING
              and appointment.updatedAt < :stuckSince
            """)
    int releaseStuckAppointments(@Param("stuckSince") LocalDateTime stuckSince, @Param("now") LocalDateTime now);
}
