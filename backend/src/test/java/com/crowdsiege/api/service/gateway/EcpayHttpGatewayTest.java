package com.crowdsiege.api.service.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EcpayHttpGatewayTest {

    @Test
    void parsesDecodedKeyValuePairs() {
        assertThat(EcpayHttpGateway.parse("TradeStatus=1&ItemName=a+b%21&CustomField1="))
                .contains(Map.of("TradeStatus", "1", "ItemName", "a b!", "CustomField1", ""));
    }

    @Test
    void malformedPercentEncodingIsAFailedQueryNotAnException() {
        assertThat(EcpayHttpGateway.parse("TradeStatus=1&ItemName=%ZZ")).isEmpty();
        assertThat(EcpayHttpGateway.parse("A=%")).isEmpty();
    }

    @Test
    void rejectsBodiesWithoutKeyValueShape() {
        assertThat(EcpayHttpGateway.parse("<html>error</html>")).isEmpty();
        assertThat(EcpayHttpGateway.parse("")).isEmpty();
        assertThat(EcpayHttpGateway.parse(null)).isEmpty();
    }
}
