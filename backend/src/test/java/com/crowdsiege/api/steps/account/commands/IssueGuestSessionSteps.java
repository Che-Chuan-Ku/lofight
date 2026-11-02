package com.crowdsiege.api.steps.account.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class IssueGuestSessionSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("以裝置識別建立guest身分：")
    public void 以裝置識別建立guest身分(DataTable dataTable) {
        String deviceId = dataTable.asMaps().get(0).get("deviceId");
        scenarioContext.setLastResponse(apiClient.post("/sessions/actions/issue-guest",
                RequestBodies.of("deviceId", deviceId), null));
    }
}
