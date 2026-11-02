package com.crowdsiege.api.service.gateway;

/**
 * Verified third-party identity; only the subject is used (the provider name is never stored, D05).
 */
public record ThirdPartyIdentity(String subject) {
}
