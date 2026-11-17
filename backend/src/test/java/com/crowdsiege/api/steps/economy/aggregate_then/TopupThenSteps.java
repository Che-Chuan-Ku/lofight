package com.crowdsiege.api.steps.economy.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.cucumber.StubEcpayGateway;
import com.crowdsiege.api.model.EventKind;
import com.crowdsiege.api.model.LedgerSource;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.service.EcpayCheckMac;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.EcpayTestClient;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class TopupThenSteps {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CoinLedgerEntryRepository coinLedgerEntryRepository;

    @Autowired
    private PlayerEventRepository playerEventRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private EcpayTestClient ecpay;

    @Autowired
    private EcpayProperties ecpayProperties;

    @Autowired
    private StubEcpayGateway gateway;

    @Then("綠界收到的回應應為 {string}")
    public void 綠界收到的回應應為(String body) {
        ResponseEntity<String> response = scenarioContext.getLastResponse();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(body);
        assertThat(String.valueOf(response.getHeaders().getContentType())).startsWith("text/plain");
    }

    @Then("所有綠界同時送出的回應應為 {string}")
    public void 所有同時送出的回應應為(String body) {
        assertThat(scenarioContext.getConcurrentResponses()).isNotEmpty()
                .allSatisfy(response -> assertThat(response.getBody()).isEqualTo(body));
    }

    @Then("儲值單 {string} 的狀態應為 {string}")
    public void 儲值單的狀態應為(String order, String status) {
        assertThat(ecpay.order(order).getPaymentStatus().name()).as("儲值單 %s 狀態", order).isEqualTo(status);
    }

    @Then("儲值單 {string} 的失敗原因應為 {string}")
    public void 儲值單的失敗原因應為(String order, String reason) {
        assertThat(ecpay.order(order).getFailureReason()).isEqualTo(reason);
    }

    @Then("儲值單 {string} 的通知次數應為 {int}")
    public void 儲值單的通知次數應為(String order, int count) {
        assertThat(ecpay.order(order).getNotifyCount()).isEqualTo(count);
    }

    @Then("儲值單 {string} 記錄的綠界交易編號為 {string}、付款方式為 {string}，通知次數仍為 {int}")
    public void 儲值單記錄的綠界交易資料(String order, String tradeNo, String paymentType, int count) {
        assertThat(ecpay.order(order).getTradeNo()).isEqualTo(tradeNo);
        assertThat(ecpay.order(order).getPaymentType()).isEqualTo(paymentType);
        assertThat(ecpay.order(order).getNotifyCount()).isEqualTo(count);
    }

    @Then("儲值單 {string} 的付款到期時間應為 {string}")
    public void 儲值單的付款到期時間應為(String order, String iso) {
        assertThat(ecpay.order(order).getPaymentExpireAt()).isEqualTo(CellValues.instant(iso));
    }

    @Then("儲值單 {string} 標記為模擬付款：{word}")
    public void 儲值單標記為模擬付款(String order, String expected) {
        assertThat(ecpay.order(order).isSimulated()).isEqualTo(Boolean.parseBoolean(expected));
    }

    @Then("儲值單 {string} 記錄的通知內容不應包含簽章與金鑰")
    public void 儲值單通知內容不含簽章與金鑰(String order) {
        PaymentOrder paymentOrder = ecpay.order(order);
        String payload = jdbcTemplate.queryForObject(
                "SELECT last_notify_payload FROM payment_orders WHERE id = ?", String.class, paymentOrder.getId());
        assertThat(String.valueOf(payload)).doesNotContain("CheckMacValue").doesNotContain(ecpayProperties.getHashKey())
                .doesNotContain(ecpayProperties.getHashIv());
    }

    @Then("玩家 {string} 的儲值單筆數應為 {int}")
    public void 玩家的儲值單筆數應為(String playerName, int count) {
        Long playerId = contextHelper.getPlayerId(playerName);
        assertThat(paymentOrderRepository.findAll().stream().filter(o -> o.getPlayerId().equals(playerId)).count())
                .isEqualTo(count);
    }

    @Then("儲值單總數應為 {int}")
    public void 儲值單總數應為(int count) {
        assertThat(paymentOrderRepository.count()).isEqualTo(count);
    }

    @Then("玩家 {string} 的累計贊助額應為 {int}")
    public void 玩家的累計贊助額應為(String playerName, int total) {
        Player player = playerRepository.findById(contextHelper.getPlayerId(playerName)).orElseThrow();
        assertThat(player.getSponsoredTotalTwd()).as("玩家 %s 累計贊助額", playerName).isEqualTo(total);
    }

    @Then("玩家 {string} 的首次贊助時間應為 {string}")
    public void 玩家的首次贊助時間應為(String playerName, String iso) {
        Player player = playerRepository.findById(contextHelper.getPlayerId(playerName)).orElseThrow();
        if ("null".equals(iso)) {
            assertThat(player.getFirstSponsoredAt()).isNull();
        } else {
            assertThat(player.getFirstSponsoredAt()).isEqualTo(CellValues.instant(iso));
        }
    }

    @Then("玩家 {string} 應有 {int} 筆 TOPUP 帳本且皆綁定儲值單")
    public void 玩家應有筆TOPUP帳本(String playerName, int count) {
        var entries = coinLedgerEntryRepository.findByPlayerIdAndLedgerSource(
                contextHelper.getPlayerId(playerName), LedgerSource.TOPUP);
        assertThat(entries).hasSize(count).allSatisfy(entry -> {
            assertThat(entry.getPaymentOrderId()).isNotNull();
            assertThat(entry.getCoinDelta()).isPositive();
        });
    }

    @Then("玩家 {string} 應有 {int} 筆 TOPUP 事件")
    public void 玩家應有筆TOPUP事件(String playerName, int count) {
        Long playerId = contextHelper.getPlayerId(playerName);
        assertThat(playerEventRepository.findAll().stream()
                .filter(e -> e.getEventKind() == EventKind.TOPUP && e.getPlayerId().equals(playerId)).count())
                .isEqualTo(count);
    }

    @Then("綠界查單次數應為 {int}")
    public void 綠界查單次數應為(int count) {
        assertThat(gateway.callCount()).isEqualTo(count);
    }

    @Then("回應的結帳欄位簽章應正確")
    public void 回應的結帳欄位簽章應正確() {
        Map<String, String> fields = checkoutFields();
        assertThat(EcpayCheckMac.verify(fields, ecpayProperties.getHashKey(), ecpayProperties.getHashIv()))
                .as("checkout.fields CheckMacValue").isTrue();
    }

    @Then("回應的結帳欄位 {string} 應為 {string}")
    public void 回應的結帳欄位應為(String field, String value) {
        assertThat(checkoutFields().get(field)).isEqualTo(value);
    }

    @Then("回應的結帳欄位 {string} 應以 {string} 開頭")
    public void 回應的結帳欄位應以開頭(String field, String prefix) {
        assertThat(checkoutFields().get(field)).startsWith(prefix);
    }

    @Then("回應的結帳欄位不應包含 {string}")
    public void 回應的結帳欄位不應包含(String field) {
        assertThat(checkoutFields()).doesNotContainKey(field);
    }

    @Then("回應原文不應包含金鑰")
    public void 回應原文不應包含金鑰() {
        assertThat(scenarioContext.getLastResponse().getBody())
                .doesNotContain(ecpayProperties.getHashKey()).doesNotContain(ecpayProperties.getHashIv());
    }

    @Then("回應的結帳網址應為 {string}")
    public void 回應的結帳網址應為(String url) {
        assertThat(responseReader.data().path("checkout").path("actionUrl").asText()).isEqualTo(url);
    }

    @Then("瀏覽器應被導向 {string}")
    public void 瀏覽器應被導向(String location) {
        assertThat((Integer) scenarioContext.getMemo("browserStatus")).isEqualTo(303);
        assertThat((String) scenarioContext.getMemo("browserLocation")).isEqualTo(location);
    }

    private Map<String, String> checkoutFields() {
        JsonNode fields = responseReader.data().path("checkout").path("fields");
        assertThat(fields.isObject()).as("checkout.fields 應為物件，實際 %s", responseReader.body()).isTrue();
        Map<String, String> map = new LinkedHashMap<>();
        fields.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
        return map;
    }
}
