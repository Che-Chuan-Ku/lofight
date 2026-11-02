package com.crowdsiege.api.steps.common.aggregate_given;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.CellValues;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

public class ClockGivenSteps {

    @Autowired
    private MutableClock clock;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private JwtHelper jwtHelper;

    /**
     * Moves the clock; a token given as "valid" earlier (e.g. in Background) is re-issued at the new time so it stays
     * valid, matching the Gherkin intent.
     */
    @Given("現在時間為 {string}")
    public void 現在時間為(String isoTime) {
        clock.setInstant(CellValues.instant(isoTime));
        Long validTokenPlayerId = scenarioContext.getMemo(AccessTokenGivenSteps.VALID_TOKEN_PLAYER_ID);
        if (validTokenPlayerId != null) {
            scenarioContext.setAccessToken(jwtHelper.generateToken(validTokenPlayerId));
        }
    }
}
