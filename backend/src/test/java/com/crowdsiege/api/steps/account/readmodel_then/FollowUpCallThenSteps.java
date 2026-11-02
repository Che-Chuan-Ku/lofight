package com.crowdsiege.api.steps.account.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Then steps that issue an extra call to prove a token is no longer usable.
 */
public class FollowUpCallThenSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("以玩家 {string} 原有的存取權杖查詢玩家進度應失敗，錯誤代碼為 {string}")
    public void 以玩家原有的存取權杖查詢玩家進度應失敗錯誤代碼為(String playerName, String code) {
        String originalToken = jwtHelper.generateToken(contextHelper.getPlayerId(playerName));
        assertFailure(apiClient.get("/players/me", originalToken), code);
    }

    @Then("以換發權杖 {string} 再次換發應失敗，錯誤代碼為 {string}")
    public void 以換發權杖再次換發應失敗錯誤代碼為(String refreshToken, String code) {
        assertFailure(apiClient.post("/sessions/actions/refresh", RequestBodies.of("refreshToken", refreshToken),
                null), code);
    }

    private void assertFailure(ResponseEntity<String> response, String code) {
        assertThat(response.getStatusCode().is4xxClientError())
                .as("預期失敗（4XX），實際 %s %s", response.getStatusCode(), response.getBody()).isTrue();
        assertThat(responseReader.parse(response).path("code").asText()).as("錯誤代碼").isEqualTo(code);
    }
}
