package com.crowdsiege.api.steps.common.aggregate_given;

import com.crowdsiege.api.config.GameProperties;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

public class BoardVersionGivenSteps {

    @Autowired
    private GameProperties gameProperties;

    @Given("後端設定的目前版本前兩碼為 {string}")
    public void 後端設定的目前版本前兩碼為(String boardVersion) {
        gameProperties.setBoardVersion(boardVersion);
    }
}
