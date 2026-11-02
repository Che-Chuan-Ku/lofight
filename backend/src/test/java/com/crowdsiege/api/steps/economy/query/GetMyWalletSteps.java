package com.crowdsiege.api.steps.economy.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class GetMyWalletSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("查詢錢包餘額")
    public void 查詢錢包餘額() {
        scenarioContext.setLastResponse(apiClient.get("/players/me/wallet", scenarioContext.getAccessToken()));
    }
}
