package com.crowdsiege.api.steps.economy.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.steps.helpers.PlayerStateAssertions;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class CoinLedgerThenSteps {

    @Autowired
    private CoinLedgerEntryRepository coinLedgerEntryRepository;

    @Autowired
    private PlayerStateAssertions playerStateAssertions;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("玩家 {string} 的金幣帳本應依序包含：")
    public void 玩家的金幣帳本應依序包含(String playerName, DataTable dataTable) {
        playerStateAssertions.assertLedgerStartsWith(contextHelper.getPlayerId(playerName), dataTable.asMaps());
    }

    @Then("玩家 {string} 的金幣帳本筆數應為 {int}")
    public void 玩家的金幣帳本筆數應為(String playerName, int count) {
        assertThat(coinLedgerEntryRepository.countByPlayerId(contextHelper.getPlayerId(playerName)))
                .as("玩家 %s 帳本筆數", playerName).isEqualTo(count);
    }
}
