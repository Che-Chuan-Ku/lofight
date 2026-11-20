package com.crowdsiege.api.steps.economy.aggregate_given;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.config.TopupProperties;
import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.cucumber.StubEcpayGateway;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.EcpayTestClient;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Top-up settings (reset per scenario by DatabaseCleanupHook), existing orders and the QueryTradeInfo stub.
 */
public class TopupGivenSteps {

    @Autowired
    private EcpayProperties ecpayProperties;

    @Autowired
    private TopupProperties topupProperties;

    @Autowired
    private StubEcpayGateway gateway;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private EcpayTestClient ecpay;

    @Autowired
    private MutableClock clock;

    @Given("綠界特店編號未設定")
    public void 綠界特店編號未設定() {
        ecpayProperties.setMerchantId("");
    }

    @Given("綠界環境為 {string}")
    public void 綠界環境為(String env) {
        ecpayProperties.setEnv(env);
    }

    @Given("贊助者名框第 2 級門檻設定為 {int}")
    public void 贊助者名框第2級門檻設定為(int amountTwd) {
        topupProperties.setSponsorTier2MinTwd(amountTwd);
    }

    @Given("系統中有以下儲值單：")
    public void 系統中有以下儲值單(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            Long playerId = contextHelper.getPlayerId(row.get("玩家"));
            int amount = Integer.parseInt(row.get("amountTwd"));
            Instant createdAt = CellValues.instant(row.get("createdAt"));
            PaymentOrder order = paymentOrderRepository.save(new PaymentOrder(playerId, row.get("訂單"),
                    "TWD_" + amount, amount, topupProperties.getCoinsPerTwd() * amount, ecpayProperties.getEnv(),
                    row.get("merchantId") == null ? ecpayProperties.getMerchantId() : row.get("merchantId"),
                    createdAt));
            jdbcTemplate.update("UPDATE payment_orders SET payment_status = ?, failure_reason = ?, "
                            + "payment_expire_at = ? WHERE id = ?",
                    (short) com.crowdsiege.api.model.PaymentStatus.valueOf(row.get("status")).getCode(),
                    row.get("failureReason"),
                    row.get("paymentExpireAt") == null ? null
                            : java.sql.Timestamp.from(CellValues.instant(row.get("paymentExpireAt"))),
                    order.getId());
        }
    }

    @Given("玩家 {string} 的累計贊助額為 {int}")
    public void 玩家的累計贊助額為(String playerName, int total) {
        jdbcTemplate.update("UPDATE players SET sponsored_total_twd = ?, first_sponsored_at = ? WHERE id = ?", total,
                total > 0 ? java.sql.Timestamp.from(clock.instant()) : null, contextHelper.getPlayerId(playerName));
    }

    @Given("綠界查單 {string} 回應 TradeStatus 為 {string}、TradeAmt 為 {int}")
    public void 綠界查單回應(String order, String tradeStatus, int tradeAmt) {
        script(order, tradeStatus, tradeAmt, null, true, false, false);
    }

    @Given("綠界查單 {string} 回應 TradeStatus 為 {string}、TradeAmt 為 {int}、CustomField1 為玩家 {string}")
    public void 綠界查單回應含玩家(String order, String tradeStatus, int tradeAmt, String playerName) {
        script(order, tradeStatus, tradeAmt, String.valueOf(contextHelper.getPlayerId(playerName)), true, false, false);
    }

    @Given("綠界查單 {string} 回應簽章錯誤")
    public void 綠界查單回應簽章錯誤(String order) {
        script(order, "1", ecpay.order(order).getAmountTwd(), null, false, false, false);
    }

    @Given("綠界查單 {string} 回應 HTTP 403")
    public void 綠界查單回應403(String order) {
        script(order, "1", 0, null, true, false, true);
    }

    @Given("允許使用綠界公開測試特店")
    public void 允許使用綠界公開測試特店() {
        ecpayProperties.setAllowTestMerchant(true);
    }

    @Given("不允許使用綠界公開測試特店")
    public void 不允許使用綠界公開測試特店() {
        ecpayProperties.setAllowTestMerchant(false);
    }

    @Given("綠界查單 {string} 連線失敗")
    public void 綠界查單連線失敗(String order) {
        script(order, "1", 0, null, true, true, false);
    }

    private void script(String order, String tradeStatus, int tradeAmt, String customField1, boolean validMac,
                        boolean failure, boolean forbidden) {
        PaymentOrder paymentOrder = ecpay.order(order);
        String owner = customField1 == null ? String.valueOf(paymentOrder.getPlayerId()) : customField1;
        gateway.script(paymentOrder.getMerchantTradeNo(),
                new StubEcpayGateway.Scripted(tradeStatus, tradeAmt, owner, validMac, failure, forbidden));
    }
}
