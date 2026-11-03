package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Tier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Long> {

    List<LeaderboardEntry> findByBoardVersionAndTier(String boardVersion, Tier tier);

    Optional<LeaderboardEntry> findByBoardVersionAndTierAndPlayerId(String boardVersion, Tier tier, Long playerId);

    List<LeaderboardEntry> findByPlayerId(Long playerId);

    /**
     * Seasons that ever had a score (INF-S15).
     */
    @Query("SELECT DISTINCT e.boardVersion FROM LeaderboardEntry e")
    List<String> findDistinctBoardVersions();
}
