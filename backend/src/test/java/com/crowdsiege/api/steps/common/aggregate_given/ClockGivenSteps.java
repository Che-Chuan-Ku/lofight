package com.crowdsiege.api.steps.common.aggregate_given;

import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.steps.helpers.CellValues;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

public class ClockGivenSteps {

    @Autowired
    private MutableClock clock;

    @Given("現在時間為 {string}")
    public void 現在時間為(String isoTime) {
        clock.setInstant(CellValues.instant(isoTime));
    }
}
