package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.model.RunState;
import com.crowdsiege.api.model.Tier;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunRepository extends JpaRepository<Run, Long> {

    long countByPlayerId(Long playerId);

    /**
     * Row lock so a run is settled at most once even under concurrent submissions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Run r WHERE r.id = :runId")
    Optional<Run> findByIdForUpdate(@Param("runId") Long runId);

    @Query("SELECT MAX(r.bossesKilled) FROM Run r WHERE r.playerId = :playerId AND r.tier = :tier "
            + "AND r.runState = :runState")
    Optional<Integer> findBestBossesKilled(@Param("playerId") Long playerId, @Param("tier") Tier tier,
                                           @Param("runState") RunState runState);
}
