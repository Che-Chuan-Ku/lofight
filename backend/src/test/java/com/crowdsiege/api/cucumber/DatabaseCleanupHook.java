package com.crowdsiege.api.cucumber;

import com.crowdsiege.api.config.AdRewardProperties;
import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.config.TopupProperties;
import com.crowdsiege.api.steps.economy.aggregate_given.SeasonSettlementGivenSteps;
import io.cucumber.java.Before;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Resets database, stubs and mutable settings before every scenario.
 */
public class DatabaseCleanupHook {

    private static final String TRUNCATE_ALL = "TRUNCATE TABLE season_settlements, player_events, payment_orders, "
            + "leaderboard_entries, run_boss_kills, "
            + "coin_ledger_entries, buff_inventories, runs, wallets, refresh_tokens, player_identities, players "
            + "RESTART IDENTITY CASCADE";

    private static String defaultBuildVersion;
    private static String defaultBoardVersion;
    private static List<String> defaultBannedWords;
    private static String defaultPublisherId;
    private static boolean adRewardDefaultsCaptured;
    private static String defaultMerchantId;
    private static String defaultEcpayEnv;
    private static boolean defaultAllowTest;
    private static int defaultTier2MinTwd;
    private static int defaultMaxOpenOrders;
    private static boolean topupDefaultsCaptured;

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

    @Autowired
    private AdRewardProperties adRewardProperties;

    @Autowired
    private EcpayProperties ecpayProperties;

    @Autowired
    private TopupProperties topupProperties;

    @Autowired
    private StubEcpayGateway ecpayGateway;

    @Before(order = 0)
    public void resetState() {
        jdbcTemplate.execute(SeasonSettlementGivenSteps.FAILURE_TRIGGER_CLEANUP);
        jdbcTemplate.execute(TRUNCATE_ALL);
        clock.reset();
        nicknameGenerator.reset();
        googleIdTokenVerifier.reset();
        appleIdentityTokenVerifier.reset();
        resetGameProperties();
        resetAdRewardProperties();
        resetTopupProperties();
        ecpayGateway.reset();
    }

    private void resetTopupProperties() {
        if (!topupDefaultsCaptured) {
            defaultMerchantId = ecpayProperties.getMerchantId();
            defaultEcpayEnv = ecpayProperties.getEnv();
            defaultAllowTest = ecpayProperties.isAllowTestMerchant();
            defaultTier2MinTwd = topupProperties.getSponsorTier2MinTwd();
            defaultMaxOpenOrders = topupProperties.getMaxOpenOrders();
            topupDefaultsCaptured = true;
        }
        ecpayProperties.setMerchantId(defaultMerchantId);
        ecpayProperties.setEnv(defaultEcpayEnv);
        ecpayProperties.setAllowTestMerchant(defaultAllowTest);
        topupProperties.setSponsorTier2MinTwd(defaultTier2MinTwd);
        topupProperties.setMaxOpenOrders(defaultMaxOpenOrders);
    }

    private void resetAdRewardProperties() {
        if (!adRewardDefaultsCaptured) {
            defaultPublisherId = adRewardProperties.getPublisherId();
            adRewardDefaultsCaptured = true;
        }
        adRewardProperties.setPublisherId(defaultPublisherId);
    }

    private void resetGameProperties() {
        if (defaultBoardVersion == null) {
            defaultBuildVersion = gameProperties.getBuildVersion();
            defaultBoardVersion = gameProperties.getBoardVersion();
            defaultBannedWords = new ArrayList<>(gameProperties.getBannedWords());
        }
        gameProperties.setBuildVersion(defaultBuildVersion);
        gameProperties.setBoardVersion(defaultBoardVersion);
        gameProperties.setBannedWords(new ArrayList<>(defaultBannedWords));
    }
}
