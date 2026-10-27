package com.rian.appointment_api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rian.appointment_api.exception.ProtocolUnavailableException;
import com.rian.appointment_api.support.TestAppointmentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProtocolClientTest {

    private static final String PROTOCOL_URL =
            TestAppointmentProperties.PROTOCOL_BASE_URL + TestAppointmentProperties.PROTOCOL_PATH;

    private MockRestServiceServer protocolServiceServer;
    private ProtocolClient protocolClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(TestAppointmentProperties.PROTOCOL_BASE_URL);
        protocolServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        protocolClient = new ProtocolClient(restClientBuilder.build(), TestAppointmentProperties.create());
    }

    @Test
    void returnsUuidFromExternalService() {
        protocolServiceServer.expect(requestTo(PROTOCOL_URL))
                .andRespond(withSuccess(
                        "{\"uuid\": \"4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d\"}", MediaType.APPLICATION_JSON));

        String generatedProtocol = protocolClient.generateProtocol();

        assertThat(generatedProtocol).isEqualTo("4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d");
    }

    @Test
    void throwsProtocolUnavailableWhenServiceReturnsServerError() {
        protocolServiceServer.expect(requestTo(PROTOCOL_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> protocolClient.generateProtocol())
                .isInstanceOf(ProtocolUnavailableException.class);
    }

    @Test
    void throwsProtocolUnavailableWhenUuidIsMissing() {
        protocolServiceServer.expect(requestTo(PROTOCOL_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> protocolClient.generateProtocol())
                .isInstanceOf(ProtocolUnavailableException.class)
                .hasMessageContaining("empty protocol");
    }

    @Test
    void throwsProtocolUnavailableWhenUuidIsInvalid() {
        protocolServiceServer.expect(requestTo(PROTOCOL_URL))
                .andRespond(withSuccess("{\"uuid\": \"not-a-uuid\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> protocolClient.generateProtocol())
                .isInstanceOf(ProtocolUnavailableException.class)
                .hasMessageContaining("invalid protocol");
    }
}
