package com.crowdsiege.api.cucumber;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apple gateway stub fed by "Given Apple 驗證以下 identity token 的結果為：" (Apple登入 is deferred, BDY-01=B).
 */
public class StubAppleIdentityTokenVerifier {

    private final Map<String, String> validTokens = new ConcurrentHashMap<>();

    public void registerValid(String identityToken, String subject) {
        validTokens.put(identityToken, subject);
    }

    public void reset() {
        validTokens.clear();
    }

    public Optional<String> subjectOf(String identityToken) {
        return Optional.ofNullable(validTokens.get(identityToken));
    }
}
