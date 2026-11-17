package com.crowdsiege.api.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * MerchantTradeNo = "DW" + yyMMddHHmmss (Asia/Taipei) + 6 random [0-9A-Z] = 20 characters; also the public order id.
 */
public final class MerchantTradeNos {

    public static final Pattern FORMAT = Pattern.compile("^DW[0-9A-Z]{18}$");

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(TAIPEI);
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private MerchantTradeNos() {
    }

    public static String generate(Instant now) {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return "DW" + STAMP.format(now) + suffix;
    }

    public static boolean isValid(String value) {
        return value != null && FORMAT.matcher(value).matches();
    }
}
