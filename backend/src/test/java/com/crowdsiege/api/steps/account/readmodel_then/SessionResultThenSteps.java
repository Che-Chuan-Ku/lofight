package com.crowdsiege.api.steps.account.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class SessionResultThenSteps {

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("回應中的存取權杖應可識別為玩家 {string}")
    public void 回應中的存取權杖應可識別為玩家(String playerName) {
        assertThat(accessTokenSubject()).as("存取權杖識別的玩家")
                .isEqualTo(String.valueOf(contextHelper.getPlayerId(playerName)));
    }

    @Then("回應中的存取權杖應可識別為裝置識別 {string} 對應的玩家")
    public void 回應中的存取權杖應可識別為裝置識別對應的玩家(String deviceId) {
        Player player = playerRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new AssertionError("裝置識別 " + deviceId + " 尚無玩家"));
        assertThat(accessTokenSubject()).as("存取權杖識別的玩家").isEqualTo(String.valueOf(player.getId()));
    }

    @Then("回應中的新換發權杖到期時間應為 {string}")
    public void 回應中的新換發權杖到期時間應為(String expiresAt) {
        CellValues.assertMatches("refreshTokenExpiresAt", expiresAt,
                CellValues.jsonValue(responseReader.data().get("refreshTokenExpiresAt")));
    }

    private String accessTokenSubject() {
        String accessToken = responseReader.data().path("accessToken").asText(null);
        assertThat(accessToken).as("回應中的存取權杖").isNotBlank();
        return jwtHelper.subjectOf(accessToken);
    }
}
