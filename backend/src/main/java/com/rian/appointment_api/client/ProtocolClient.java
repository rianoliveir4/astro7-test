package com.rian.appointment_api.client;

import com.rian.appointment_api.config.AppointmentProperties;
import com.rian.appointment_api.exception.ProtocolUnavailableException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class ProtocolClient {

    private final RestClient protocolRestClient;
    private final AppointmentProperties appointmentProperties;

    public String generateProtocol() {
        UuidResponse uuidResponse = requestUuid();

        if (uuidResponse == null || uuidResponse.uuid() == null || uuidResponse.uuid().isBlank()) {
            throw new ProtocolUnavailableException("External service returned an empty protocol");
        }

        try {
            return UUID.fromString(uuidResponse.uuid()).toString();
        } catch (IllegalArgumentException invalidUuidException) {
            throw new ProtocolUnavailableException(
                    "External service returned an invalid protocol: " + uuidResponse.uuid(), invalidUuidException);
        }
    }

    private UuidResponse requestUuid() {
        try {
            return protocolRestClient.get()
                    .uri(appointmentProperties.protocol().path())
                    .retrieve()
                    .body(UuidResponse.class);
        } catch (RestClientException restClientException) {
            throw new ProtocolUnavailableException(
                    "External service request failed: " + restClientException.getMessage(), restClientException);
        }
    }
}
