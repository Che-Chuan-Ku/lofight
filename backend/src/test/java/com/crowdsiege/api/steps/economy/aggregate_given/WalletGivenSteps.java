package com.crowdsiege.api.steps.economy.aggregate_given;

import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class WalletGivenSteps {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Given("玩家 {string} 的金幣餘額為 {int}")
    public void 玩家的金幣餘額為(String playerName, int coins) {
        jdbcTemplate.update("UPDATE wallets SET coins = ? WHERE player_id = ?", coins,
                contextHelper.getPlayerId(playerName));
    }
}
