package com.crowdsiege.api.steps.economy.aggregate_then;

import com.crowdsiege.api.service.AdRewardService;
import com.crowdsiege.api.service.AdRewardService.AdRewardQuota;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

public class AdRewardThenSteps {

    @Autowired
    private AdRewardService adRewardService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private ScenarioContextHelper contextHelper;

    /**
     * Daily remaining count and next claimable time derived from the player's own AD_REWARD ledger rows.
     */
    @Then("玩家 {string} 的廣告獎勵狀態應為：")
    public void 玩家的廣告獎勵狀態應為(String playerName, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        AdRewardQuota quota = transactionTemplate.execute(
                status -> adRewardService.quotaOf(playerId, clock.instant()));
        Map<String, Object> actual = new HashMap<>();
        actual.put("remainingCount", quota.remainingCount());
        actual.put("nextClaimableAt", quota.nextClaimableAt());
        CellValues.assertRowMatches(dataTable.asMaps().get(0), actual);
    }
}
