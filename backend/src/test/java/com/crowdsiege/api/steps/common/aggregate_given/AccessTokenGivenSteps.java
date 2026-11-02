package com.crowdsiege.api.steps.common.aggregate_given;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessTokenGivenSteps {

    /**
     * Memo key of the player holding "a valid access token"; the token is re-issued when the scenario moves the
     * clock so it stays valid (see ClockGivenSteps).
     */
    public static final String VALID_TOKEN_PLAYER_ID = "validTokenPlayerId";

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private JwtHelper jwtHelper;

    @Given("目前以玩家 {string} 的有效存取權杖操作")
    public void 目前以玩家的有效存取權杖操作(String playerName) {
        Long playerId = contextHelper.getPlayerId(playerName);
        scenarioContext.setAccessToken(jwtHelper.generateToken(playerId));
        scenarioContext.putMemo(VALID_TOKEN_PLAYER_ID, playerId);
    }

    @Given("目前以玩家 {string} 已過期的存取權杖操作")
    public void 目前以玩家已過期的存取權杖操作(String playerName) {
        scenarioContext.setAccessToken(jwtHelper.generateExpiredToken(contextHelper.getPlayerId(playerName)));
        scenarioContext.putMemo(VALID_TOKEN_PLAYER_ID, null);
    }

    @Given("目前未攜帶存取權杖")
    public void 目前未攜帶存取權杖() {
        scenarioContext.setAccessToken(null);
        scenarioContext.putMemo(VALID_TOKEN_PLAYER_ID, null);
    }
}
