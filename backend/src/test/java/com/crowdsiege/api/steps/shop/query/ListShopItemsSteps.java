package com.crowdsiege.api.steps.shop.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class ListShopItemsSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("瀏覽商店")
    public void 瀏覽商店() {
        scenarioContext.setLastResponse(apiClient.get("/shop-items", scenarioContext.getAccessToken()));
    }
}
