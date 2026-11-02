package com.crowdsiege.api.steps.economy.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.ConcurrentRequests;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class ClaimAdRewardSteps {

    private static final String PATH = "/players/me/ad-rewards";

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("領取廣告獎勵：")
    public void 領取廣告獎勵(DataTable dataTable) {
        scenarioContext.setLastResponse(claim(dataTable.asMaps().get(0).get("deviceId"),
                scenarioContext.getAccessToken()));
    }

    @When("同時送出以下廣告獎勵領取：")
    public void 同時送出以下廣告獎勵領取(DataTable dataTable) {
        String accessToken = scenarioContext.getAccessToken();
        List<Supplier<ResponseEntity<String>>> requests = dataTable.asMaps().stream()
                .<Supplier<ResponseEntity<String>>>map(row -> () -> claim(row.get("deviceId"), accessToken))
                .toList();
        scenarioContext.setConcurrentResponses(
                ConcurrentRequests.runTogether(requests));
    }

    private ResponseEntity<String> claim(String deviceId, String accessToken) {
        return apiClient.post(PATH, RequestBodies.of("deviceId", deviceId), accessToken);
    }
}
