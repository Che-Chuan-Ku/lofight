package com.crowdsiege.api.steps.account.commands;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class AppleLoginSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private JwtHelper jwtHelper;

    @When("以 Apple 登入：")
    public void 以Apple登入(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        String guestName = row.get("攜帶guest");
        String guestToken = guestName == null ? null : jwtHelper.generateToken(contextHelper.getPlayerId(guestName));
        scenarioContext.setLastResponse(apiClient.post("/sessions/actions/login-apple",
                RequestBodies.of("identityToken", row.get("identityToken")), guestToken));
    }
}
