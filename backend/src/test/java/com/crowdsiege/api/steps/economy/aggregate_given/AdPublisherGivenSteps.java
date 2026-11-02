package com.crowdsiege.api.steps.economy.aggregate_given;

import com.crowdsiege.api.config.AdRewardProperties;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Toggles the site-level AdSense publisher id (ADSENSE_PUBLISHER_ID); reset per scenario by DatabaseCleanupHook.
 */
public class AdPublisherGivenSteps {

    private static final String TEST_PUBLISHER_ID = "ca-pub-0000000000000000";

    @Autowired
    private AdRewardProperties adRewardProperties;

    @Given("廣告發布商 ID 已設定")
    public void 廣告發布商ID已設定() {
        adRewardProperties.setPublisherId(TEST_PUBLISHER_ID);
    }

    @Given("廣告發布商 ID 未設定")
    public void 廣告發布商ID未設定() {
        adRewardProperties.setPublisherId(null);
    }
}
