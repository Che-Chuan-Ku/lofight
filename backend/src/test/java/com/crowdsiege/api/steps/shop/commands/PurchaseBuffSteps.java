package com.crowdsiege.api.steps.shop.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.ConcurrentRequests;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class PurchaseBuffSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @When("購買 buff：")
    public void 購買buff(DataTable dataTable) {
        scenarioContext.setLastResponse(purchase(dataTable.asMaps().get(0).get("buffItem"),
                scenarioContext.getAccessToken()));
    }

    @When("同時送出以下購買：")
    public void 同時送出以下購買(DataTable dataTable) {
        String accessToken = scenarioContext.getAccessToken();
        List<Supplier<ResponseEntity<String>>> requests = dataTable.asMaps().stream()
                .<Supplier<ResponseEntity<String>>>map(row -> () -> purchase(row.get("buffItem"), accessToken))
                .toList();
        scenarioContext.setConcurrentResponses(ConcurrentRequests.runTogether(requests));
    }

    private ResponseEntity<String> purchase(String shopItemId, String accessToken) {
        return apiClient.post("/shop-items/" + shopItemId + "/actions/purchase", null, accessToken);
    }
}
