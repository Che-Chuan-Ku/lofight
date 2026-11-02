package com.crowdsiege.api.steps.economy.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class WalletThenSteps {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("玩家 {string} 的金幣餘額應為 {int}")
    public void 玩家的金幣餘額應為(String playerName, int coins) {
        assertThat(walletRepository.findCoinsByPlayerId(contextHelper.getPlayerId(playerName)))
                .as("玩家 %s 金幣餘額", playerName).contains(coins);
    }
}
