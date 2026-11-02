package com.crowdsiege.api.steps.shop.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class ListBuffInventoriesSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("查詢 buff 庫存")
    public void 查詢buff庫存() {
        scenarioContext.setLastResponse(apiClient.get("/players/me/buff-inventories",
                scenarioContext.getAccessToken()));
    }
}
