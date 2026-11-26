package com.beathub.multiarenas.delivery.auth.config;

import com.beathub.multiarenas.delivery.auth.audit.AuditContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestClientConfig {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private ClientHttpRequestInterceptor correlationInterceptor() {
        return (request, body, execution) -> {
            String correlationId = AuditContextHolder.getCorrelationId();
            if (correlationId != null && !correlationId.isBlank()
                    && request.getHeaders().getFirst(CORRELATION_HEADER) == null) {
                request.getHeaders().add(CORRELATION_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(correlationInterceptor()));
        return restTemplate;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor(correlationInterceptor())
                .build();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
