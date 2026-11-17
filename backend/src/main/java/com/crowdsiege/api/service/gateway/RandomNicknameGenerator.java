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

    /**
     * True when the name has exactly the shape next() produces ("勇者" + a number in 100..999). Used by the donation
     * matcher: a system default name identifies nobody. Callers pass an already normalized (NFKC) name.
     */
    public static boolean isDefaultName(String name) {
        if (name == null || !name.startsWith(PREFIX)) {
            return false;
        }
        String suffix = name.substring(PREFIX.length());
        if (suffix.length() != String.valueOf(MIN_SUFFIX + SUFFIX_RANGE - 1).length()) {
            return false;
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (suffix.charAt(i) < '0' || suffix.charAt(i) > '9') {
                return false;
            }
        }
        int number = Integer.parseInt(suffix);
        return number >= MIN_SUFFIX && number < MIN_SUFFIX + SUFFIX_RANGE;
    }

    @Override
    public String next() {
        return PREFIX + (MIN_SUFFIX + random.nextInt(SUFFIX_RANGE));
    }
}
