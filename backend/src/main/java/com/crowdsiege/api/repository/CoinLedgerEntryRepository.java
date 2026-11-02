package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.CoinLedgerEntry;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinLedgerEntryRepository extends JpaRepository<CoinLedgerEntry, Long> {

    /**
     * Newest first; ties on time are broken by id (GAP-11).
     */
    Page<CoinLedgerEntry> findByPlayerIdOrderByCreatedAtDescIdDesc(Long playerId, Pageable pageable);

    List<CoinLedgerEntry> findByPlayerIdOrderByCreatedAtDescIdDesc(Long playerId);

    long countByPlayerId(Long playerId);
}
