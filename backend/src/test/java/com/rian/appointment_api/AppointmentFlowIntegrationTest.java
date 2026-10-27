package com.rian.appointment_api;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rian.appointment_api.repository.AppointmentRepository;
import com.rian.appointment_api.client.ProtocolClient;
import com.rian.appointment_api.exception.ProtocolUnavailableException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@DirtiesContext
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "appointment.job.interval=200ms",
        "appointment.processing.initial-backoff=100ms"
})
class AppointmentFlowIntegrationTest {

    private static final String FIRST_PATIENT_CPF = "529.982.247-25";
    private static final String SECOND_PATIENT_CPF = "111.444.777-35";
    private static final Duration MAX_WAIT = Duration.ofSeconds(15);

    @Container
    @ServiceConnection
    static MySQLContainer mysqlContainer = new MySQLContainer("mysql:8.4");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private ProtocolClient protocolClient;

    @BeforeEach
    void cleanDatabase() {
        appointmentRepository.deleteAll();
    }

    @Test
    void completesAppointmentThroughScheduledProcessing() throws Exception {
        String generatedProtocol = UUID.randomUUID().toString();
        when(protocolClient.generateProtocol()).thenReturn(generatedProtocol);

        long appointmentId = createAppointment("Ana Souza", FIRST_PATIENT_CPF);

        await().atMost(MAX_WAIT).untilAsserted(() -> findAppointment(appointmentId)
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.protocol").value(generatedProtocol)));
    }

    @Test
    void retriesFailedAppointmentWithoutLosingIt() throws Exception {
        String generatedProtocol = UUID.randomUUID().toString();
        when(protocolClient.generateProtocol())
                .thenThrow(new ProtocolUnavailableException("External service request failed: 500"))
                .thenReturn(generatedProtocol);

        long appointmentId = createAppointment("Ana Souza", FIRST_PATIENT_CPF);

        await().atMost(MAX_WAIT).untilAsserted(() -> findAppointment(appointmentId)
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.protocol").value(generatedProtocol)));
    }

    @Test
    void slowAppointmentDoesNotBlockOtherAppointments() throws Exception {
        CountDownLatch slowRequestRelease = new CountDownLatch(1);
        AtomicInteger protocolRequestCount = new AtomicInteger();
        when(protocolClient.generateProtocol()).thenAnswer(invocation -> {
            if (protocolRequestCount.getAndIncrement() == 0) {
                slowRequestRelease.await(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
            }
            return UUID.randomUUID().toString();
        });

        long slowAppointmentId = createAppointment("Ana Souza", FIRST_PATIENT_CPF);
        await().atMost(MAX_WAIT).untilAsserted(() -> findAppointment(slowAppointmentId)
                .andExpect(jsonPath("$.status").value("PROCESSING")));

        long fastAppointmentId = createAppointment("Bruno Lima", SECOND_PATIENT_CPF);
        await().atMost(MAX_WAIT).untilAsserted(() -> findAppointment(fastAppointmentId)
                .andExpect(jsonPath("$.status").value("COMPLETED")));
        findAppointment(slowAppointmentId).andExpect(jsonPath("$.status").value("PROCESSING"));

        slowRequestRelease.countDown();
        await().atMost(MAX_WAIT).untilAsserted(() -> findAppointment(slowAppointmentId)
                .andExpect(jsonPath("$.status").value("COMPLETED")));
    }

    @Test
    void allowsNewAppointmentOnlyAfterPreviousOneFinishes() throws Exception {
        CountDownLatch protocolRequestRelease = new CountDownLatch(1);
        when(protocolClient.generateProtocol()).thenAnswer(invocation -> {
            protocolRequestRelease.await(MAX_WAIT.toSeconds(), TimeUnit.SECONDS);
            return UUID.randomUUID().toString();
        });

        long firstAppointmentId = createAppointment("Ana Souza", FIRST_PATIENT_CPF);

        postAppointment("Ana Souza", FIRST_PATIENT_CPF)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.activeAppointmentId").value(firstAppointmentId));

        protocolRequestRelease.countDown();
        await().atMost(MAX_WAIT).untilAsserted(() -> findAppointment(firstAppointmentId)
                .andExpect(jsonPath("$.status").value("COMPLETED")));

        postAppointment("Ana Souza", FIRST_PATIENT_CPF).andExpect(status().isCreated());
    }

    private long createAppointment(String patientName, String patientCpf) throws Exception {
        String responseBody = postAppointment(patientName, patientCpf)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(responseBody, "$.id")).longValue();
    }

    private ResultActions postAppointment(String patientName, String patientCpf) throws Exception {
        return mockMvc.perform(post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"patientName": "%s", "patientCpf": "%s"}
                        """.formatted(patientName, patientCpf)));
    }

    private ResultActions findAppointment(long appointmentId) throws Exception {
        return mockMvc.perform(get("/api/appointments/{appointmentId}", appointmentId))
                .andExpect(status().isOk());
    }
}
