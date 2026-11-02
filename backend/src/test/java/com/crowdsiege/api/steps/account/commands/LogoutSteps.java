package com.crowdsiege.api.steps.account.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class LogoutSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("以換發權杖登出：")
    public void 以換發權杖登出(DataTable dataTable) {
        String refreshToken = dataTable.asMaps().get(0).get("refreshToken");
        scenarioContext.setLastResponse(apiClient.post("/sessions/actions/logout",
                RequestBodies.of("refreshToken", refreshToken), scenarioContext.getAccessToken()));
    }
}
