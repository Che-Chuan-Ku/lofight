package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LedgerSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    /**
     * Rows of one source in [from, to); AD_REWARD uses it for the UTC daily count
     * (idx_coin_ledger_entries_player_id_ledger_source_created_at).
     */
    long countByPlayerIdAndLedgerSourceAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long playerId, LedgerSource ledgerSource, Instant from, Instant to);

    List<CoinLedgerEntry> findByPlayerIdAndLedgerSource(Long playerId, LedgerSource ledgerSource);

    Optional<CoinLedgerEntry> findFirstByPlayerIdAndLedgerSourceOrderByCreatedAtDescIdDesc(
            Long playerId, LedgerSource ledgerSource);
}
