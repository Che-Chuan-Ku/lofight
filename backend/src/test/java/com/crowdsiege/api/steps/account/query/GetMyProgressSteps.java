package com.crowdsiege.api.steps.account.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class GetMyProgressSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("查詢玩家進度")
    public void 查詢玩家進度() {
        scenarioContext.setLastResponse(apiClient.get("/players/me", scenarioContext.getAccessToken()));
    }
}
