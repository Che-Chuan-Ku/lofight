package com.crowdsiege.api.steps.economy.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.model.Donation;
import com.crowdsiege.api.repository.DonationRepository;
import com.crowdsiege.api.steps.helpers.ConcurrentRequests;
import com.crowdsiege.api.steps.helpers.EcpayDonateTestClient;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Streamer-donation notification: settings, the ECPay-side POST and the donation record assertions.
 */
public class DonateSteps {

    @Autowired
    private EcpayDonateTestClient client;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private EcpayProperties ecpayProperties;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Given("綠界實況主收款連結為 {string}")
    public void 綠界實況主收款連結為(String url) {
        ecpayProperties.setDonateUrl(url);
    }

    @When("綠界送出實況主贊助通知：")
    public void 綠界送出實況主贊助通知(DataTable dataTable) {
        scenarioContext.setLastResponse(client.post(client.notice(dataTable.asMaps().get(0))));
    }

    @When("綠界重送 {int} 次相同的實況主贊助通知：")
    public void 綠界重送次相同的實況主贊助通知(int times, DataTable dataTable) {
        EcpayDonateTestClient.Notice notice = client.notice(dataTable.asMaps().get(0));
        for (int i = 0; i < times; i++) {
            scenarioContext.setLastResponse(client.post(notice));
        }
    }

    @When("綠界同時送出 {int} 次相同的實況主贊助通知：")
    public void 綠界同時送出次相同的實況主贊助通知(int times, DataTable dataTable) {
        EcpayDonateTestClient.Notice notice = client.notice(dataTable.asMaps().get(0));
        List<Supplier<ResponseEntity<String>>> requests = java.util.stream.IntStream.range(0, times)
                .<Supplier<ResponseEntity<String>>>mapToObj(i -> () -> client.post(notice)).toList();
        scenarioContext.setConcurrentResponses(ConcurrentRequests.runTogether(requests));
        scenarioContext.setLastResponse(scenarioContext.getConcurrentResponses().get(0));
    }

    @Then("贊助紀錄 {string} 的配對狀態應為 {string}")
    public void 贊助紀錄的配對狀態應為(String tradeNo, String status) {
        assertThat(find(tradeNo).getMatchStatus()).isEqualTo(status);
    }

    @Then("贊助紀錄 {string} 應綁定玩家 {string} 與一筆已入帳的儲值單")
    public void 贊助紀錄應綁定玩家(String tradeNo, String playerName) {
        Donation donation = find(tradeNo);
        assertThat(donation.getPlayerId()).isEqualTo(contextHelper.getPlayerId(playerName));
        assertThat(donation.getPaymentOrderId()).isNotNull();
    }

    @Then("贊助紀錄 {string} 不應綁定任何玩家或儲值單")
    public void 贊助紀錄不應綁定任何玩家(String tradeNo) {
        Donation donation = find(tradeNo);
        assertThat(donation.getPlayerId()).isNull();
        assertThat(donation.getPaymentOrderId()).isNull();
    }

    @Given("玩家 {string} 的錢包被移除")
    public void 玩家的錢包被移除(String playerName) {
        jdbcTemplate.update("DELETE FROM wallets WHERE player_id = ?", contextHelper.getPlayerId(playerName));
    }

    @Then("贊助紀錄共 {int} 筆且配對狀態皆為 {string}")
    public void 贊助紀錄共筆且皆為(int count, String status) {
        assertThat(donationRepository.findAll()).hasSize(count)
                .allSatisfy(d -> assertThat(d.getMatchStatus()).isEqualTo(status));
    }

    @Then("贊助紀錄 {string} 的錯誤類別應為 {string}")
    public void 贊助紀錄的錯誤類別應為(String tradeNo, String errorClass) {
        assertThat(find(tradeNo).getErrorClass()).isEqualTo(errorClass);
    }

    @Then("贊助紀錄總數應為 {int}")
    public void 贊助紀錄總數應為(int count) {
        assertThat(donationRepository.count()).isEqualTo(count);
    }

    @Then("贊助紀錄 {string} 標記為模擬付款：{word}")
    public void 贊助紀錄標記為模擬付款(String tradeNo, String expected) {
        assertThat(find(tradeNo).isSimulatePaid()).isEqualTo(Boolean.parseBoolean(expected));
    }

    @Then("贊助紀錄 {string} 記錄的內容不應包含簽章與金鑰")
    public void 贊助紀錄內容不含簽章與金鑰(String tradeNo) {
        String payload = String.valueOf(find(tradeNo).getPayload());
        assertThat(payload).doesNotContain("CheckMacValue").doesNotContain(ecpayProperties.getHashKey())
                .doesNotContain(ecpayProperties.getHashIv());
    }

    private Donation find(String tradeNo) {
        return donationRepository.findAll().stream().filter(d -> d.getTradeNo().equals(tradeNo)).findFirst()
                .orElseThrow(() -> new AssertionError("找不到贊助紀錄 " + tradeNo));
    }
}
