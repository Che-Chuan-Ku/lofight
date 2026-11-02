package com.crowdsiege.api.service;

import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Keeps one best score per (board version, tier, player) (D06, ASM-06, GAP-12).
 */
@Service
public class LeaderboardScoreService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;

    public LeaderboardScoreService(LeaderboardEntryRepository leaderboardEntryRepository) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
    }

    /**
     * Records the settled run's score on the run's own board version; ties keep the earlier score.
     */
    public void recordSettledRun(Run run, Instant now) {
        LeaderboardEntry candidate = new LeaderboardEntry(run.getPlayerId(), run.getBoardVersion(), run.getTier(),
                run.getId(), run.getVictory(), run.getGameSeconds(), run.getSettledAt(), now);
        leaderboardEntryRepository
                .findByBoardVersionAndTierAndPlayerId(run.getBoardVersion(), run.getTier(), run.getPlayerId())
                .ifPresentOrElse(current -> {
                    if (LeaderboardRanking.isStrictlyBetter(candidate.isVictory(), candidate.getGameSeconds(),
                            current)) {
                        current.replaceScore(candidate, now, run.getPlayerId());
                    }
                }, () -> leaderboardEntryRepository.save(candidate));
    }
}
