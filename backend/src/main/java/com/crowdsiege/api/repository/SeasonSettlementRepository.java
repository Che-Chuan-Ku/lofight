package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.SeasonSettlement;
import com.crowdsiege.api.model.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonSettlementRepository extends JpaRepository<SeasonSettlement, Long> {

    boolean existsByBoardVersionAndTier(String boardVersion, Tier tier);
}
