package com.crowdsiege.api.steps.account.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.cucumber.StubGoogleIdTokenVerifier;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * A guest binds Google: Google login carrying the guest's token (merge into a new member, guest voided).
 * The player label keeps pointing at the guest record.
 */
public class BindGoogleSteps {

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private StubGoogleIdTokenVerifier googleIdTokenVerifier;

    @When("玩家 {string} 綁定 Google 帳號")
    public void 玩家綁定Google帳號(String playerName) {
        Long guestId = contextHelper.getPlayerId(playerName);
        String idToken = "bind-google-" + guestId;
        googleIdTokenVerifier.registerValid(idToken, "google-sub-" + guestId);
        ResponseEntity<String> response = apiClient.post("/sessions/actions/login-google",
                RequestBodies.of("idToken", idToken), jwtHelper.generateToken(guestId));
        assertThat(response.getStatusCode().is2xxSuccessful()).as("綁定 Google 失敗：%s", response.getBody())
                .isTrue();
    }
}
