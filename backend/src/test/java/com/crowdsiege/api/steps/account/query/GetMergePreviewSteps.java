package com.crowdsiege.api.steps.account.query;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

public class GetMergePreviewSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("查詢合併預覽：")
    public void 查詢合併預覽(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/merge-previews");
        if (row.get("loginProvider") != null) {
            uri.queryParam("loginProvider", row.get("loginProvider"));
        }
        Map<String, String> headers = new HashMap<>();
        if (row.get("providerToken") != null) {
            headers.put("X-Provider-Token", row.get("providerToken"));
        }
        scenarioContext.setLastResponse(apiClient.get(uri.toUriString(), scenarioContext.getAccessToken(), headers));
    }
}
