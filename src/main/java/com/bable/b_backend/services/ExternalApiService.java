package com.bable.b_backend.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

// External API service using the rest client

@Service
public class ExternalApiService {

    private final RestClient.Builder restClientBuilder;
    private final String baseUrl;

    // Constructor for this instance of External API
    public ExternalApiService(
            RestClient.Builder builder,
            @Value("${app.ext.url:}") String baseUrl
    ) {
        this.restClientBuilder = builder;
        this.baseUrl = baseUrl;
    }

    // Check for missing environment variable
    public String uploadImage(String imgUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Missing configuration: app.ext.url");
        }

    // Building API request
        Map<String, String> reqBody = Map.of("url", imgUrl);
        return restClientBuilder
                .baseUrl(baseUrl)
                .build()
                .post()
                .uri("/api/image")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reqBody)
                .retrieve()
                .body(String.class);
    }

}
