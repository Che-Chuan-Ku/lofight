package com.crowdsiege.api.cucumber;

import io.cucumber.spring.ScenarioScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Per-scenario state: last response, caller access token and natural-key -> id mapping.
 */
@Component
@ScenarioScope
public class ScenarioContext {

    private ResponseEntity<String> lastResponse;
    private List<ResponseEntity<String>> concurrentResponses = new ArrayList<>();
    private final Map<String, Object> ids = new HashMap<>();
    private final Map<String, Object> memo = new HashMap<>();
    private String accessToken;

    public ResponseEntity<String> getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ResponseEntity<String> lastResponse) {
        this.lastResponse = lastResponse;
    }

    public List<ResponseEntity<String>> getConcurrentResponses() {
        return concurrentResponses;
    }

    public void setConcurrentResponses(List<ResponseEntity<String>> concurrentResponses) {
        this.concurrentResponses = concurrentResponses;
    }

    public void putId(String key, Object id) {
        ids.put(key, id);
    }

    @SuppressWarnings("unchecked")
    public <T> T getId(String key) {
        return (T) ids.get(key);
    }

    public boolean hasId(String key) {
        return ids.containsKey(key);
    }

    /**
     * Reverse lookup: the natural key (without prefix) whose id equals the given value.
     */
    public String labelOf(String keyPrefix, Object id) {
        return ids.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(keyPrefix) && entry.getValue().equals(id))
                .map(entry -> entry.getKey().substring(keyPrefix.length()))
                .findFirst()
                .orElse(null);
    }

    public void putMemo(String key, Object value) {
        memo.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMemo(String key) {
        return (T) memo.get(key);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
