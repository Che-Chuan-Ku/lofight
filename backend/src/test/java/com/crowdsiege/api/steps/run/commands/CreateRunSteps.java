package com.crowdsiege.api.steps.run.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ConcurrentRequests;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class CreateRunSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("開局：")
    public void 開局(DataTable dataTable) {
        scenarioContext.setLastResponse(createRun(dataTable.asMaps().get(0), scenarioContext.getAccessToken()));
    }

    @When("同時送出以下開局：")
    public void 同時送出以下開局(DataTable dataTable) {
        String accessToken = scenarioContext.getAccessToken();
        List<Supplier<ResponseEntity<String>>> requests = dataTable.asMaps().stream()
                .<Supplier<ResponseEntity<String>>>map(row -> () -> createRun(row, accessToken))
                .toList();
        scenarioContext.setConcurrentResponses(ConcurrentRequests.runTogether(requests));
    }

    private ResponseEntity<String> createRun(Map<String, String> row, String accessToken) {
        List<String> buffItems = CellValues.csv(row.get("buffItems"));
        Map<String, Object> body = RequestBodies.of("tier", row.get("tier"),
                "buffItems", buffItems.isEmpty() ? null : buffItems);
        return apiClient.post("/runs", body, accessToken);
    }
}
