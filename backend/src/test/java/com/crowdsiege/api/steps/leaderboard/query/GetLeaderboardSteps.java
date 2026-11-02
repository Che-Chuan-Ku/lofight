package com.crowdsiege.api.steps.leaderboard.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

public class GetLeaderboardSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("查詢排行榜：")
    public void 查詢排行榜(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/leaderboards");
        if (row.get("tier") != null) {
            uri.queryParam("tier", row.get("tier"));
        }
        if (row.get("boardVersion") != null) {
            uri.queryParam("boardVersion", row.get("boardVersion"));
        }
        scenarioContext.setLastResponse(apiClient.get(uri.toUriString(), scenarioContext.getAccessToken()));
    }
}
