package com.crowdsiege.api.steps.ops.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

public class GetEventStatisticsSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("營運者查詢事件統計：")
    public void 營運者查詢事件統計(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/internal/ops/event-statistics");
        if (row.get("from") != null) {
            uri.queryParam("from", row.get("from"));
        }
        if (row.get("to") != null) {
            uri.queryParam("to", row.get("to"));
        }
        scenarioContext.setLastResponse(apiClient.get(uri.toUriString(), null));
    }
}
