package com.crowdsiege.api.steps.helpers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Thin HTTP helper over TestRestTemplate; paths are relative to the api prefix (/api/csg/v1).
 */
@Component
public class ApiClient {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Value("${api.prefix}")
    private String apiPrefix;

    public ResponseEntity<String> get(String path, String accessToken) {
        return send(HttpMethod.GET, path, null, accessToken, Map.of());
    }

    public ResponseEntity<String> get(String path, String accessToken, Map<String, String> headers) {
        return send(HttpMethod.GET, path, null, accessToken, headers);
    }

    public ResponseEntity<String> post(String path, Object body, String accessToken) {
        return send(HttpMethod.POST, path, body, accessToken, Map.of());
    }

    public ResponseEntity<String> patch(String path, Object body, String accessToken) {
        return send(HttpMethod.PATCH, path, body, accessToken, Map.of());
    }

    public ResponseEntity<String> send(HttpMethod method, String path, Object body, String accessToken,
                                       Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        extraHeaders.forEach(headers::set);
        return testRestTemplate.exchange(apiPrefix + path, method, new HttpEntity<>(body, headers), String.class);
    }
}
