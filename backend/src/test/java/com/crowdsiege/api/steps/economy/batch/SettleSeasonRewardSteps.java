package com.crowdsiege.api.steps.economy.batch;

import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.service.SeasonRewardService;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @batch trigger: a backend (re)start is simulated by deriving the current season from the given build version
 * and calling the same settlement entry point the ApplicationReadyEvent listener uses (no context restart).
 */
public class SettleSeasonRewardSteps {

    private static final int INSTANCE_COUNT = 2;

    @Autowired
    private GameProperties gameProperties;

    @Autowired
    private SeasonRewardService seasonRewardService;

    @When("後端以建置版號 {string} 啟動")
    public void 後端以建置版號啟動(String buildVersion) {
        startWith(buildVersion);
        seasonRewardService.settlePendingSeasons(gameProperties.currentSeason());
    }

    @When("後端以建置版號 {string} 重新啟動")
    public void 後端以建置版號重新啟動(String buildVersion) {
        後端以建置版號啟動(buildVersion);
    }

    @When("兩個後端實例同時以建置版號 {string} 啟動")
    public void 兩個後端實例同時以建置版號啟動(String buildVersion) throws Exception {
        startWith(buildVersion);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(INSTANCE_COUNT);
        try {
            List<Future<?>> instances = new ArrayList<>();
            for (int i = 0; i < INSTANCE_COUNT; i++) {
                instances.add(executor.submit(() -> {
                    start.await();
                    seasonRewardService.settlePendingSeasons(gameProperties.currentSeason());
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> instance : instances) {
                instance.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void startWith(String buildVersion) {
        gameProperties.setBuildVersion(buildVersion);
        gameProperties.afterPropertiesSet();
    }
}
