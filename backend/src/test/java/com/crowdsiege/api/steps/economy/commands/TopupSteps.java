package com.crowdsiege.api.steps.economy.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.ConcurrentRequests;
import com.crowdsiege.api.steps.helpers.EcpayTestClient;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import com.crowdsiege.api.model.Player;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Top-up commands: create order, query order, and the ECPay server / browser callbacks.
 */
public class TopupSteps {

    private static final String ORDERS = "/players/me/topup-orders";

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private EcpayTestClient ecpay;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Autowired
    private com.crowdsiege.api.repository.PlayerRepository playerRepository;

    @Autowired
    private com.crowdsiege.api.steps.helpers.ScenarioContextHelper contextHelper;

    @Autowired
    private com.crowdsiege.api.cucumber.MutableClock clock;

    @When("建立儲值單：")
    public void 建立儲值單(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        Map<String, Object> body = new LinkedHashMap<>(RequestBodies.of("packageId", row.get("packageId")));
        // Forged extras must be ignored by the server.
        if (row.get("amountTwd") != null) {
            body.put("amountTwd", Integer.parseInt(row.get("amountTwd")));
        }
        if (row.get("coins") != null) {
            body.put("coins", Integer.parseInt(row.get("coins")));
        }
        ResponseEntity<String> response = apiClient.post(ORDERS, body, scenarioContext.getAccessToken());
        scenarioContext.setLastResponse(response);
        String id = responseReader.data().path("topupOrderId").asText(null);
        if (id != null) {
            scenarioContext.putMemo(EcpayTestClient.LAST_ORDER_KEY, id);
        }
    }

    @When("查詢儲值單 {string}")
    public void 查詢儲值單(String order) {
        scenarioContext.setLastResponse(
                apiClient.get(ORDERS + "/" + ecpay.resolveTradeNo(order), scenarioContext.getAccessToken()));
    }

    @When("綠界送出付款通知：")
    public void 綠界送出付款通知(DataTable dataTable) {
        scenarioContext.setLastResponse(ecpay.postCallback("return", ecpay.returnForm(dataTable.asMaps().get(0))));
    }

    @When("綠界重送 {int} 次相同的付款通知：")
    public void 綠界重送次相同的付款通知(int times, DataTable dataTable) {
        Map<String, String> form = ecpay.returnForm(dataTable.asMaps().get(0));
        for (int i = 0; i < times; i++) {
            scenarioContext.setLastResponse(ecpay.postCallback("return", form));
        }
    }

    @When("綠界同時送出 {int} 次相同的付款通知：")
    public void 綠界同時送出次相同的付款通知(int times, DataTable dataTable) {
        Map<String, String> form = ecpay.returnForm(dataTable.asMaps().get(0));
        List<Supplier<ResponseEntity<String>>> requests = java.util.stream.IntStream.range(0, times)
                .<Supplier<ResponseEntity<String>>>mapToObj(i -> () -> ecpay.postCallback("return", form))
                .toList();
        scenarioContext.setConcurrentResponses(ConcurrentRequests.runTogether(requests));
        scenarioContext.setLastResponse(scenarioContext.getConcurrentResponses().get(0));
    }

    @When("綠界送出取號通知：")
    public void 綠界送出取號通知(DataTable dataTable) {
        scenarioContext.setLastResponse(
                ecpay.postCallback("payment-info", ecpay.paymentInfoForm(dataTable.asMaps().get(0))));
    }

    /**
     * Reproduces a stale whole-row write: the player entity is read first, a payment is credited meanwhile (separate
     * transaction via the callback), then the stale entity is saved by a rename. The total must survive.
     */
    @When("玩家 {string} 的資料被讀取後綠界付款通知入帳，再以舊資料改暱稱為 {string}：")
    public void 以舊資料改暱稱(String playerName, String nickname, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        Map<String, String> form = ecpay.returnForm(dataTable.asMaps().get(0));
        transactionTemplate.executeWithoutResult(status -> {
            Player stale = playerRepository.findById(playerId).orElseThrow();
            scenarioContext.setLastResponse(ecpay.postCallback("return", form));
            stale.rename(nickname, clock.instant());
            playerRepository.saveAndFlush(stale);
        });
    }

    @When("玩家瀏覽器以付款結果導回：")
    public void 玩家瀏覽器以付款結果導回(DataTable dataTable) {
        Map<String, String> form = ecpay.returnForm(dataTable.asMaps().get(0));
        HttpResponse<String> response = ecpay.postWithoutFollowing("order-result", form);
        scenarioContext.putMemo("browserStatus", response.statusCode());
        scenarioContext.putMemo("browserLocation", response.headers().firstValue("Location").orElse(null));
    }

    @When("玩家瀏覽器送出偽造的導回網址：")
    public void 玩家瀏覽器送出偽造的導回網址(DataTable dataTable) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("MerchantTradeNo", dataTable.asMaps().get(0).get("MerchantTradeNo"));
        HttpResponse<String> response = ecpay.postWithoutFollowing("order-result", form);
        scenarioContext.putMemo("browserStatus", response.statusCode());
        scenarioContext.putMemo("browserLocation", response.headers().firstValue("Location").orElse(null));
    }
}
