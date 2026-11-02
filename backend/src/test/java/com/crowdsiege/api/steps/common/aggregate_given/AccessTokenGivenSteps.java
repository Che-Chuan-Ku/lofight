package com.crowdsiege.api.steps.common.aggregate_given;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessTokenGivenSteps {

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private JwtHelper jwtHelper;

    @Given("目前以玩家 {string} 的有效存取權杖操作")
    public void 目前以玩家的有效存取權杖操作(String playerName) {
        scenarioContext.setAccessToken(jwtHelper.generateToken(contextHelper.getPlayerId(playerName)));
    }

    @Given("目前以玩家 {string} 已過期的存取權杖操作")
    public void 目前以玩家已過期的存取權杖操作(String playerName) {
        scenarioContext.setAccessToken(jwtHelper.generateExpiredToken(contextHelper.getPlayerId(playerName)));
    }

    @Given("目前未攜帶存取權杖")
    public void 目前未攜帶存取權杖() {
        scenarioContext.setAccessToken(null);
    }
}
