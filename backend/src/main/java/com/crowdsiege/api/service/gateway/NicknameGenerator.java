package com.crowdsiege.api.service.gateway;

/**
 * Produces the system default nickname for new players (D05, e.g. 勇者248).
 */
public interface NicknameGenerator {

    String next();
}
