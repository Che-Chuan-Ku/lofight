package com.crowdsiege.api.cucumber;

import com.crowdsiege.api.service.gateway.GoogleIdTokenVerifier;
import com.crowdsiege.api.service.gateway.ThirdPartyIdentity;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google gateway stub fed by "Given Google 驗證以下 id_token 的結果為：".
 */
public class StubGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    private final Map<String, ThirdPartyIdentity> validTokens = new ConcurrentHashMap<>();

    public void registerValid(String idToken, String subject) {
        validTokens.put(idToken, new ThirdPartyIdentity(subject));
    }

    public void reset() {
        validTokens.clear();
    }

    @Override
    public Optional<ThirdPartyIdentity> verify(String idToken) {
        return Optional.ofNullable(validTokens.get(idToken));
    }
}
