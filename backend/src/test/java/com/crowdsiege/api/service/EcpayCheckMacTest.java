package com.crowdsiege.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * U1-U5: CheckMacValue against the official ECPay example (https://developers.ecpay.com.tw/2902/, test HashKey/IV from
 * https://developers.ecpay.com.tw/2856/) and a Python quote_plus(safe="-_.!*()") reference (with ~ forced to %7E: .NET and ECPay keep it encoded).
 */
class EcpayCheckMacTest {

    private static final String HASH_KEY = "pwFHCqoQZGmho4w6";
    private static final String HASH_IV = "EkRm7iFT261dpevs";

    private static Map<String, String> officialExample() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("TradeDesc", "促銷方案");
        params.put("PaymentType", "aio");
        params.put("MerchantTradeDate", "2023/03/12 15:30:23");
        params.put("MerchantTradeNo", "ecpay20230312153023");
        params.put("MerchantID", "3002607");
        params.put("ReturnURL", "https://www.ecpay.com.tw/receive.php");
        params.put("ItemName", "Apple iphone 15");
        params.put("TotalAmount", "30000");
        params.put("ChoosePayment", "ALL");
        params.put("EncryptType", "1");
        return params;
    }

    @Test
    void u1_officialExampleMatchesPublishedValue() {
        assertThat(EcpayCheckMac.compute(officialExample(), HASH_KEY, HASH_IV))
                .isEqualTo("6C51C9E6888DE861FD62FB1DD17029FC742634498FD813DC43D4243B5685B840");
    }

    @Test
    void u2_specialCharactersMatchPythonReference() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ItemName", "Tea & Co. (50%) ~a!b*c-d_e.f'g +h/i");
        params.put("TradeDesc", "金幣 DayWar");
        params.put("MerchantID", "3002607");
        params.put("CustomField1", "");
        assertThat(EcpayCheckMac.compute(params, HASH_KEY, HASH_IV))
                .isEqualTo("0E74C65E33A48131E9853913C8E9127619A6B7FF3929C6FB179E60C8CDB54A90");
    }

    @Test
    void u3_sortingIsCaseInsensitive() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("c", "3");
        params.put("B", "2");
        params.put("a", "1");
        // a, B, c order; plain ASCII order (B, a, c) would give a different value.
        assertThat(EcpayCheckMac.compute(params, HASH_KEY, HASH_IV))
                .isEqualTo("7B31682AB822D8BB204076ECAB8749AADEA298E9FBE39B5BA1E3B5531B9490BE");
    }

    @Test
    void u4_verifyRejectsAnyModification() {
        Map<String, String> signed = officialExample();
        signed.put("CheckMacValue", EcpayCheckMac.compute(signed, HASH_KEY, HASH_IV));
        assertThat(EcpayCheckMac.verify(signed, HASH_KEY, HASH_IV)).isTrue();

        Map<String, String> changedValue = new LinkedHashMap<>(signed);
        changedValue.put("TotalAmount", "1");
        assertThat(EcpayCheckMac.verify(changedValue, HASH_KEY, HASH_IV)).isFalse();

        Map<String, String> addedField = new LinkedHashMap<>(signed);
        addedField.put("CustomField1", "");
        assertThat(EcpayCheckMac.verify(addedField, HASH_KEY, HASH_IV)).isFalse();

        Map<String, String> removedField = new LinkedHashMap<>(signed);
        removedField.remove("ItemName");
        assertThat(EcpayCheckMac.verify(removedField, HASH_KEY, HASH_IV)).isFalse();

        Map<String, String> missingMac = new LinkedHashMap<>(officialExample());
        assertThat(EcpayCheckMac.verify(missingMac, HASH_KEY, HASH_IV)).isFalse();

        assertThat(EcpayCheckMac.verify(signed, "wrongKey", HASH_IV)).isFalse();
    }

    @Test
    void u5_verifyAcceptsLowerCaseReceivedValue() {
        Map<String, String> signed = officialExample();
        signed.put("CheckMacValue",
                "6c51c9e6888de861fd62fb1dd17029fc742634498fd813dc43d4243b5685b840");
        assertThat(EcpayCheckMac.verify(signed, HASH_KEY, HASH_IV)).isTrue();
    }

    @Test
    void emptyValuesAreIncludedInTheSignature() {
        Map<String, String> withEmpty = officialExample();
        withEmpty.put("CustomField3", "");
        assertThat(EcpayCheckMac.compute(withEmpty, HASH_KEY, HASH_IV))
                .isNotEqualTo(EcpayCheckMac.compute(officialExample(), HASH_KEY, HASH_IV));
    }
}
