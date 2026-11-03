package com.crowdsiege.api.service;

import com.crowdsiege.api.config.GameProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Triggers the season reward batch once the application is ready (Flyway already migrated), on a background
 * thread so serving requests is never delayed.
 */
@Component
public class SeasonRewardStartup {

    private static final Logger log = LoggerFactory.getLogger(SeasonRewardStartup.class);

    private final SeasonRewardService seasonRewardService;
    private final GameProperties gameProperties;

    public SeasonRewardStartup(SeasonRewardService seasonRewardService, GameProperties gameProperties) {
        this.seasonRewardService = seasonRewardService;
        this.gameProperties = gameProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Thread worker = new Thread(this::settle, "season-reward-settlement");
        worker.setDaemon(true);
        worker.start();
    }

    private void settle() {
        try {
            log.info("season_reward_settlement_started currentSeason={} buildVersion={}",
                    gameProperties.getBoardVersion(), gameProperties.getBuildVersion());
            seasonRewardService.settlePendingSeasons(gameProperties.currentSeason());
        } catch (RuntimeException ex) {
            log.error("season_reward_settlement_aborted", ex);
        }
    }
}
