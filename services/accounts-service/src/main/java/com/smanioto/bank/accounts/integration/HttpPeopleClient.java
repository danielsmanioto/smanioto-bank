package com.smanioto.bank.accounts.integration;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        var response = restClient.get()
            .uri("/people/{customerId}/exists", customerId)
            .retrieve()
            .body(CustomerExistsPayload.class);
        return response != null && response.exists();
    }

    private record CustomerExistsPayload(boolean exists) {
    }
}
