package com.crowdsiege.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crowdsiege.api.service.EcpayDonateCrypto.MacMatch;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Official examples: decryption https://developers.ecpay.com.tw/41032/ (test key pair), CheckMacValue
 * https://developers.ecpay.com.tw/41068/.
 */
class EcpayDonateCryptoTest {

    private static final String TEST_KEY = "pwFHCqoQZGmho4w6";
    private static final String TEST_IV = "EkRm7iFT261dpevs";
    private static final String UPPER_CIPHER =
            "o4TJSHkQBM1bogbn5BNFRofCVTfsQjoqv/TX8DKn757fe5AoYzoalYmrMsGXTiwxGpI8NsE2vu4tScAwISx8kw==";
    private static final String LOWER_CIPHER =
            "5/2ZKxAmcnaUHMUv67dYpSSg5J9m0r4Lrxgznl4DHffjASG04O0ENFHm/qhTvQ8gHrzIWuJr5iGQlrHBF1OFGw==";

    private static final String MAC_KEY = "7b53896b742849d3";
    private static final String MAC_IV = "37a0ad3c6ffa428b";
    private static final String MAC_DATA = "{\"MerchantID\":\"3085676\",\"MerchantTradeNo\":\"CX202202221540568521\"}";
    private static final String MAC_EXPECTED = "CE67BBD259EE38BA1C7FB7CC88C3BD91D3F082B46EAEBD4E4E5F2184CB23349A";

    @Test
    void decryptsTheOfficialExampleWithUpperCaseEncoding() {
        assertThat(EcpayDonateCrypto.decrypt(UPPER_CIPHER, TEST_KEY, TEST_IV))
                .isEqualTo("{\"Name\":\"Test\",\"ID\":\"A123456789\"}");
    }

    @Test
    void decryptsTheOfficialExampleWithLowerCaseEncoding() {
        assertThat(EcpayDonateCrypto.decrypt(LOWER_CIPHER, TEST_KEY, TEST_IV))
                .isEqualTo("{\"Name\":\"Test\",\"ID\":\"A123456789\"}");
    }

    @Test
    void restoresPlusSignsTurnedIntoSpacesByFormParsing() {
        assertThat(EcpayDonateCrypto.decrypt(UPPER_CIPHER.replace('+', ' '), TEST_KEY, TEST_IV))
                .contains("\"Test\"");
    }

    @Test
    void rejectsGarbageAndWrongKey() {
        assertThatThrownBy(() -> EcpayDonateCrypto.decrypt("not base64 !!", TEST_KEY, TEST_IV))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EcpayDonateCrypto.decrypt(UPPER_CIPHER, MAC_KEY, MAC_IV))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundTripsChineseText() throws Exception {
        String json = "{\"PatronName\":\"勇者小明\",\"PatronNote\":\"加油 + 打氣!\"}";
        String urlEncoded = java.net.URLEncoder.encode(json, StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(TEST_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(TEST_IV.getBytes(StandardCharsets.UTF_8)));
        String data = Base64.getEncoder().encodeToString(cipher.doFinal(urlEncoded.getBytes(StandardCharsets.UTF_8)));
        assertThat(EcpayDonateCrypto.decrypt(data, TEST_KEY, TEST_IV)).isEqualTo(json);
    }

    @Test
    void computesTheOfficialCheckMacValue() {
        assertThat(EcpayDonateCrypto.checkMac(MAC_DATA, MAC_KEY, MAC_IV)).isEqualTo(MAC_EXPECTED);
    }

    @Test
    void verifiesAgainstPlaintextOrCiphertextAndReportsWhich() {
        assertThat(EcpayDonateCrypto.verify(MAC_EXPECTED, MAC_DATA, "other", MAC_KEY, MAC_IV))
                .isEqualTo(MacMatch.PLAINTEXT);
        assertThat(EcpayDonateCrypto.verify(MAC_EXPECTED.toLowerCase(), "other", MAC_DATA, MAC_KEY, MAC_IV))
                .isEqualTo(MacMatch.CIPHERTEXT);
        assertThat(EcpayDonateCrypto.verify(MAC_EXPECTED, "x", "y", MAC_KEY, MAC_IV)).isEqualTo(MacMatch.NONE);
        assertThat(EcpayDonateCrypto.verify(null, MAC_DATA, MAC_DATA, MAC_KEY, MAC_IV)).isEqualTo(MacMatch.NONE);
        assertThat(EcpayDonateCrypto.verify("", MAC_DATA, MAC_DATA, MAC_KEY, MAC_IV)).isEqualTo(MacMatch.NONE);
    }

    @Test
    void rawUrlEncodeKeepsTildeAndEncodesSpaceAndStar() {
        assertThat(EcpayDonateCrypto.rawUrlEncode("a b*~(!)")).isEqualTo("a%20b%2A~%28%21%29");
    }

    @Test
    void ciphertextMacStillMatchesWhenAFormParserTurnedPlusIntoSpace() throws Exception {
        // search for a payload whose Base64 ciphertext contains '+', then sign the ciphertext (the CIPHERTEXT variant)
        String data = null;
        for (int i = 0; i < 200 && data == null; i++) {
            String candidate = encrypt("{\"n\":" + i + ",\"PatronName\":\"勇者小明\"}");
            if (candidate.contains("+")) {
                data = candidate;
            }
        }
        assertThat(data).as("a ciphertext containing '+'").isNotNull();
        String mac = EcpayDonateCrypto.checkMac(data, TEST_KEY, TEST_IV);
        String mangled = data.replace('+', ' ');
        EcpayDonateCrypto.Verified verified = EcpayDonateCrypto.decryptAndVerify(mangled, mac, TEST_KEY, TEST_IV);
        assertThat(verified.match()).isEqualTo(MacMatch.CIPHERTEXT);
        assertThat(verified.plaintext()).contains("勇者小明");
        assertThat(EcpayDonateCrypto.restoreCipher(mangled)).isEqualTo(data);
    }

    @Test
    void plaintextMacIsReportedForTheDecryptedJson() throws Exception {
        String json = "{\"a\":1}";
        String data = encrypt(json);
        String mac = EcpayDonateCrypto.checkMac(json, TEST_KEY, TEST_IV);
        assertThat(EcpayDonateCrypto.decryptAndVerify(data, mac, TEST_KEY, TEST_IV).match())
                .isEqualTo(MacMatch.PLAINTEXT);
        assertThat(EcpayDonateCrypto.decryptAndVerify(data, "0".repeat(64), TEST_KEY, TEST_IV).match())
                .isEqualTo(MacMatch.NONE);
    }

    private static String encrypt(String json) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(TEST_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(TEST_IV.getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(
                java.net.URLEncoder.encode(json, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
    }
}
