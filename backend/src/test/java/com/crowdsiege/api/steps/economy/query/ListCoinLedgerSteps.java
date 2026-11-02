package com.crowdsiege.api.steps.economy.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

public class ListCoinLedgerSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("查詢金幣帳本：")
    public void 查詢金幣帳本(DataTable dataTable) {
        String page = dataTable.asMaps().get(0).get("page");
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/players/me/coin-ledger-entries");
        if (page != null) {
            uri.queryParam("page", page);
        }
        scenarioContext.setLastResponse(apiClient.get(uri.toUriString(), scenarioContext.getAccessToken()));
    }
}
