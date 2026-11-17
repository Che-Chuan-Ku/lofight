package com.crowdsiege.api.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * ECPay streamer-donation notification crypto: AES-128-CBC/PKCS7 decryption of the Data field (Base64 ciphertext, key and
 * IV = HashKey / HashIV, result is a URL-encoded JSON string) and the CheckMacValue of that API
 * (https://developers.ecpay.com.tw/41032/ and /41068/). Pure functions.
 */
public final class EcpayDonateCrypto {

    /**
     * Which "Data" string the received CheckMacValue was computed over (the docs do not say; both are accepted).
     */
    public enum MacMatch {
        PLAINTEXT, CIPHERTEXT, NONE
    }

    /**
     * Plaintext JSON and which Data string the signature matched.
     */
    public record Verified(String plaintext, MacMatch match) {
    }

    private EcpayDonateCrypto() {
    }

    /**
     * The Data ciphertext as ECPay sent it: a form POST may turn '+' into ' ', so spaces become '+' again. Used for
     * decryption AND for the ciphertext variant of CheckMacValue.
     */
    public static String restoreCipher(String data) {
        return data.trim().replace(' ', '+');
    }

    /**
     * Decrypts and verifies in one step (throws IllegalArgumentException when Data cannot be decrypted).
     */
    public static Verified decryptAndVerify(String data, String received, String hashKey, String hashIv) {
        String cipherText = restoreCipher(data);
        String plaintext = decrypt(cipherText, hashKey, hashIv);
        return new Verified(plaintext, verify(received, plaintext, cipherText, hashKey, hashIv));
    }

    /**
     * Decrypts the Data field to the JSON text. Throws IllegalArgumentException when it cannot be decrypted.
     */
    public static String decrypt(String data, String hashKey, String hashIv) {
        try {
            // A form POST may turn '+' into ' ' on the way: restore it before Base64 decoding.
            byte[] cipherText = Base64.getDecoder().decode(restoreCipher(data));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES"),
                    new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8)));
            String urlEncoded = new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
            return java.net.URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Data cannot be decrypted");
        }
    }

    /**
     * CheckMacValue = upper(SHA256(lower(rawurlencode(HashKey + data + HashIV)))).
     */
    public static String checkMac(String data, String hashKey, String hashIv) {
        String encoded = rawUrlEncode(hashKey + data + hashIv).toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Constant-time check of the received CheckMacValue against both candidate Data strings.
     */
    public static MacMatch verify(String received, String plaintext, String ciphertext, String hashKey, String hashIv) {
        if (received == null || received.isBlank()) {
            return MacMatch.NONE;
        }
        byte[] got = received.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        boolean plain = MessageDigest.isEqual(checkMac(plaintext, hashKey, hashIv).getBytes(StandardCharsets.UTF_8), got);
        boolean cipher = MessageDigest.isEqual(checkMac(ciphertext, hashKey, hashIv).getBytes(StandardCharsets.UTF_8),
                got);
        if (plain) {
            return MacMatch.PLAINTEXT;
        }
        return cipher ? MacMatch.CIPHERTEXT : MacMatch.NONE;
    }

    /**
     * RFC 3986 percent-encoding as PHP rawurlencode / .NET Uri.EscapeDataString: only A-Z a-z 0-9 - _ . ~ stay.
     */
    static String rawUrlEncode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_'
                    || c == '.' || c == '~') {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return out.toString();
    }
}
