package com.crowdsiege.api.steps.account.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateNicknameSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("修改暱稱：")
    public void 修改暱稱(DataTable dataTable) {
        String nickname = CellValues.literal(dataTable.asMaps().get(0).get("nickname"));
        scenarioContext.setLastResponse(apiClient.patch("/players/me", RequestBodies.of("nickname", nickname),
                scenarioContext.getAccessToken()));
    }
}
