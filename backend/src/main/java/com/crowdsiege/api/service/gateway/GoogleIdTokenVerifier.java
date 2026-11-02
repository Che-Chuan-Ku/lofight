package com.crowdsiege.api.service.gateway;

import java.util.Optional;

/**
 * Gateway to Google Identity: verifies an id_token (signature, aud, expiry).
 * Returns empty when verification fails for any reason (BDY-04).
 */
public interface GoogleIdTokenVerifier {

    Optional<ThirdPartyIdentity> verify(String idToken);
}
