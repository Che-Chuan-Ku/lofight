package com.crowdsiege.api.cucumber;

import com.crowdsiege.api.config.GameProperties;
import io.cucumber.java.Before;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Resets database, stubs and mutable settings before every scenario.
 */
public class DatabaseCleanupHook {

    private static final String TRUNCATE_ALL = "TRUNCATE TABLE player_events, leaderboard_entries, run_boss_kills, "
            + "coin_ledger_entries, buff_inventories, runs, wallets, refresh_tokens, player_identities, players "
            + "RESTART IDENTITY CASCADE";

    private static String defaultBoardVersion;
    private static List<String> defaultBannedWords;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    @Autowired
    private StubNicknameGenerator nicknameGenerator;

    @Autowired
    private StubGoogleIdTokenVerifier googleIdTokenVerifier;

    @Autowired
    private StubAppleIdentityTokenVerifier appleIdentityTokenVerifier;

    @Autowired
    private GameProperties gameProperties;

    @Before(order = 0)
    public void resetState() {
        jdbcTemplate.execute(TRUNCATE_ALL);
        clock.reset();
        nicknameGenerator.reset();
        googleIdTokenVerifier.reset();
        appleIdentityTokenVerifier.reset();
        resetGameProperties();
    }

    private void resetGameProperties() {
        if (defaultBoardVersion == null) {
            defaultBoardVersion = gameProperties.getBoardVersion();
            defaultBannedWords = new ArrayList<>(gameProperties.getBannedWords());
        }
        gameProperties.setBoardVersion(defaultBoardVersion);
        gameProperties.setBannedWords(new ArrayList<>(defaultBannedWords));
    }
}
