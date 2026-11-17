package com.crowdsiege.api.steps.account.commands;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import io.cucumber.datatable.DataTable;
import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.steps.helpers.ConcurrentRequests;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @When("玩家 {string} 與 {string} 同時把暱稱改為 {string}")
    public void 同時改暱稱(String first, String second, String nickname) {
        // Tokens are resolved on the scenario thread (scenario-scoped beans are not usable from worker threads).
        List<String> tokens = java.util.stream.Stream.of(first, second)
                .map(name -> jwtHelper.generateToken(contextHelper.getPlayerId(name))).toList();
        List<Supplier<ResponseEntity<String>>> requests = tokens.stream()
                .<Supplier<ResponseEntity<String>>>map(token -> () -> apiClient.patch("/players/me",
                        RequestBodies.of("nickname", nickname), token))
                .toList();
        scenarioContext.setConcurrentResponses(ConcurrentRequests.runTogether(requests));
    }

    @Then("兩人之中恰有 {int} 人改名成功、{int} 人收到錯誤代碼 {string}")
    public void 恰有人成功(int ok, int failed, String code) {
        List<ResponseEntity<String>> responses = scenarioContext.getConcurrentResponses();
        long success = responses.stream().filter(r -> r.getStatusCode().is2xxSuccessful()).count();
        long rejected = responses.stream().filter(r -> r.getBody() != null && r.getBody().contains("\"" + code + "\"")).count();
        org.assertj.core.api.Assertions.assertThat(success).isEqualTo(ok);
        org.assertj.core.api.Assertions.assertThat(rejected).isEqualTo(failed);
    }
}
