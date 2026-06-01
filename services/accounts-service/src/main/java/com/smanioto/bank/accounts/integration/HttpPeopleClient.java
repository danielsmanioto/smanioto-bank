package com.smanioto.bank.accounts.integration;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpPeopleClient implements PeopleClient {

    private final RestClient restClient;

    public HttpPeopleClient(RestClient.Builder restClientBuilder,
                            @Value("${app.people-service.base-url:http://localhost:8081}") String peopleServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(peopleServiceBaseUrl)
            .build();
    }

    @Override
    public boolean customerExists(UUID customerId) {
        try {
            var response = restClient.get()
                .uri("/people/{customerId}/exists", customerId)
                .retrieve()
                .body(CustomerExistsPayload.class);
            return response != null && response.exists();
        } catch (RestClientResponseException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                return false;
            }
            throw ex;
        }
    }

    private record CustomerExistsPayload(boolean exists) {
    }
}
