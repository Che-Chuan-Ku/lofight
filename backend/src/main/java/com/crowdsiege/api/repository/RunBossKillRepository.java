package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.RunBossKill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunBossKillRepository extends JpaRepository<RunBossKill, Long> {

    List<RunBossKill> findByRunIdOrderByBossSeqAsc(Long runId);
}
