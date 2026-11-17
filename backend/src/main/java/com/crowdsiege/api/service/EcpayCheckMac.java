package com.crowdsiege.api.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * ECPay CheckMacValue (SHA256, EncryptType=1), https://developers.ecpay.com.tw/2902/. Pure functions, no state.
 */
public final class EcpayCheckMac {

    public static final String FIELD = "CheckMacValue";

    private EcpayCheckMac() {
    }

    /**
     * Computes the upper-case hex CheckMacValue of all parameters except CheckMacValue itself (empty values included).
     */
    public static String compute(Map<String, String> params, String hashKey, String hashIv) {
        TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.forEach((name, value) -> {
            if (!FIELD.equalsIgnoreCase(name)) {
                sorted.put(name, value == null ? "" : value);
            }
        });
        if (sorted.size() != countWithoutMac(params)) {
            // Two names differing only by case would silently collapse: refuse instead of signing something else.
            throw new IllegalArgumentException("Duplicate parameter names (case-insensitive)");
        }
        List<String> pairs = new ArrayList<>();
        sorted.forEach((name, value) -> pairs.add(name + "=" + value));
        String raw = "HashKey=" + hashKey + "&" + String.join("&", pairs) + "&HashIV=" + hashIv;
        return sha256Upper(dotNetUrlEncode(raw).toLowerCase(Locale.ROOT));
    }

    /**
     * Verifies the CheckMacValue carried in params (case-insensitive hex, constant-time comparison).
     * Returns false when it is missing or when parameter names collide.
     */
    public static boolean verify(Map<String, String> params, String hashKey, String hashIv) {
        String received = null;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (FIELD.equalsIgnoreCase(entry.getKey())) {
                received = entry.getValue();
            }
        }
        if (received == null || received.isBlank()) {
            return false;
        }
        String expected;
        try {
            expected = compute(params, hashKey, hashIv);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                received.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Equivalent of .NET HttpUtility.UrlEncode: space becomes '+', and - _ . ! * ( ) stay unencoded.
     */
    static String dotNetUrlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("%21", "!")
                .replace("%28", "(")
                .replace("%29", ")");
    }

    private static int countWithoutMac(Map<String, String> params) {
        return (int) params.keySet().stream().filter(name -> !FIELD.equalsIgnoreCase(name)).count();
    }

    private static String sha256Upper(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
