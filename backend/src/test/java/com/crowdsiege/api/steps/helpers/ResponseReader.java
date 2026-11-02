package com.crowdsiege.api.steps.helpers;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Parses the last response envelope ({status, code, message, data}).
 */
@Component
public class ResponseReader {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioContext scenarioContext;

    public JsonNode body() {
        return parse(scenarioContext.getLastResponse());
    }

    public JsonNode data() {
        return body().path("data");
    }

    public JsonNode parse(ResponseEntity<String> response) {
        try {
            String body = response.getBody();
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("回應不是 JSON: " + response.getBody(), ex);
        }
    }
}
