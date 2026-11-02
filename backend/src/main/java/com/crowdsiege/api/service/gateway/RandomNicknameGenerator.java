package com.crowdsiege.api.service.gateway;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Default nickname "勇者" + three digits (D05 example 勇者248); duplicates are allowed (G02).
 */
@Component
public class RandomNicknameGenerator implements NicknameGenerator {

    private static final String PREFIX = "勇者";
    private static final int MIN_SUFFIX = 100;
    private static final int SUFFIX_RANGE = 900;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String next() {
        return PREFIX + (MIN_SUFFIX + random.nextInt(SUFFIX_RANGE));
    }
}
