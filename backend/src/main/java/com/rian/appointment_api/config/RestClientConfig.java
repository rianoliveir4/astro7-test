package com.rian.appointment_api.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient protocolRestClient(AppointmentProperties appointmentProperties) {
        AppointmentProperties.Protocol protocolProperties = appointmentProperties.protocol();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(protocolProperties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(protocolProperties.readTimeout());

        return RestClient.builder()
                .baseUrl(protocolProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
