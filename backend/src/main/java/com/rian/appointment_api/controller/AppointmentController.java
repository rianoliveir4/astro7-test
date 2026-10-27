package com.rian.appointment_api.controller;

import com.rian.appointment_api.dto.AppointmentResponse;
import com.rian.appointment_api.dto.CreateAppointmentRequest;
import com.rian.appointment_api.service.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "${appointment.frontend-url}", exposedHeaders = HttpHeaders.LOCATION)
@RequiredArgsConstructor
public class AppointmentController {

    private static final int MAX_APPOINTMENTS_PER_QUERY = 50;

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest createAppointmentRequest) {
        AppointmentResponse createdAppointment = appointmentService.createAppointment(createAppointmentRequest);

        URI createdAppointmentLocation = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{appointmentId}")
                .buildAndExpand(createdAppointment.id())
                .toUri();

        return ResponseEntity.created(createdAppointmentLocation).body(createdAppointment);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse findAppointmentById(@PathVariable Long appointmentId) {
        return appointmentService.findAppointmentById(appointmentId);
    }

    @GetMapping(params = "ids")
    public List<AppointmentResponse> findAppointmentsByIds(
            @RequestParam("ids")
            @NotEmpty
            @Size(max = MAX_APPOINTMENTS_PER_QUERY)
            List<Long> appointmentIds) {
        return appointmentService.findAppointmentsByIds(appointmentIds);
    }
}
