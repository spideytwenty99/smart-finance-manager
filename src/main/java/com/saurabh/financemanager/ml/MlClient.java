package com.saurabh.financemanager.ml;

import com.saurabh.financemanager.transaction.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class MlClient {

    private final RestClient restClient;

    public MlClient(@Value("${ml.service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<MlPrediction> predictCategory(String description, TransactionType type) {
        try {
            MlPrediction prediction = restClient.post()
                    .uri("/predict/category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MlPredictionRequest(description, type.name()))
                    .retrieve()
                    .body(MlPrediction.class);
            return Optional.ofNullable(prediction);
        } catch (RestClientException e) {
            // Never break transaction creation because the ML service is down or slow
            log.warn("ML service not available ({}), using fallback", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}