package com.crowdsiege.api.service;

import com.crowdsiege.api.config.SeasonRewardProperties;
import com.crowdsiege.api.model.Season;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.repository.SeasonSettlementRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Season reward batch (D38, AMB-S2=d): at startup, pays every season that is strictly before the current one,
 * not before the first eligible season and not yet settled; each (season, tier) is an independent transaction.
 */
@Service
public class SeasonRewardService {

    private static final Logger log = LoggerFactory.getLogger(SeasonRewardService.class);

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final SeasonSettlementRepository seasonSettlementRepository;
    private final SeasonRewardProperties seasonRewardProperties;
    private final SeasonRewardPayout seasonRewardPayout;

    public SeasonRewardService(LeaderboardEntryRepository leaderboardEntryRepository,
                               SeasonSettlementRepository seasonSettlementRepository,
                               SeasonRewardProperties seasonRewardProperties,
                               SeasonRewardPayout seasonRewardPayout) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.seasonSettlementRepository = seasonSettlementRepository;
        this.seasonRewardProperties = seasonRewardProperties;
        this.seasonRewardPayout = seasonRewardPayout;
    }

    /**
     * Seasons are compared numerically by (major, minor) (INF-S12); oldest season first, BRONZE before SILVER
     * (INF-S16). A failing board is rolled back and logged without stopping the others; it is retried on the
     * next startup (INF-S14).
     */
    public void settlePendingSeasons(Season currentSeason) {
        Season firstEligible = seasonRewardProperties.firstEligible();
        List<Season> pending = leaderboardEntryRepository.findDistinctBoardVersions().stream()
                .map(Season::tryParse)
                .flatMap(Optional::stream)
                .filter(season -> season.isBefore(currentSeason) && !season.isBefore(firstEligible))
                .distinct()
                .sorted()
                .toList();
        for (Season season : pending) {
            for (Tier tier : Tier.values()) {
                settleBoard(season.key(), tier);
            }
        }
    }

    private void settleBoard(String boardVersion, Tier tier) {
        if (seasonSettlementRepository.existsByBoardVersionAndTier(boardVersion, tier)) {
            return;
        }
        try {
            seasonRewardPayout.pay(boardVersion, tier);
        } catch (DataIntegrityViolationException ex) {
            // Another instance settled this board first (unique key); nothing to do.
            log.info("season_reward_skipped boardVersion={} tier={} reason=already_settled", boardVersion, tier);
        } catch (RuntimeException ex) {
            log.error("season_reward_failed boardVersion={} tier={} - rolled back, retried on next startup",
                    boardVersion, tier, ex);
        }
    }
}
