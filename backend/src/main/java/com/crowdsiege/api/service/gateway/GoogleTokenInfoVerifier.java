package com.crowdsiege.api.service.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Verifies Google id_tokens through Google's tokeninfo endpoint (signature and expiry checked by Google),
 * then checks aud against the configured client id. Fails closed when no client id is configured.
 */
@Component
public class GoogleTokenInfoVerifier implements GoogleIdTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenInfoVerifier.class);

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String tokenInfoUrl;

    public GoogleTokenInfoVerifier(@Value("${google.client-id}") String clientId,
                                   @Value("${google.token-info-url}") String tokenInfoUrl) {
        this.clientId = clientId;
        this.tokenInfoUrl = tokenInfoUrl;
    }

    @Override
    public Optional<ThirdPartyIdentity> verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("google_verify_rejected reason=client_id_not_configured");
            return Optional.empty();
        }
        try {
            JsonNode tokenInfo = restClient.get()
                    .uri(UriComponentsBuilder.fromHttpUrl(tokenInfoUrl).queryParam("id_token", idToken).build().toUri())
                    .retrieve()
                    .body(JsonNode.class);
            if (tokenInfo == null || !clientId.equals(tokenInfo.path("aud").asText())) {
                log.warn("google_verify_rejected reason=aud_mismatch");
                return Optional.empty();
            }
            String subject = tokenInfo.path("sub").asText("");
            return subject.isBlank() ? Optional.empty() : Optional.of(new ThirdPartyIdentity(subject));
        } catch (RestClientException ex) {
            log.warn("google_verify_rejected reason=tokeninfo_error message={}", ex.getMessage());
            return Optional.empty();
        }
    }
}
