package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.Wallet;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByPlayerId(Long playerId);

    /**
     * Row lock that serializes one player's wallet-changing requests (INF-A3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.playerId = :playerId")
    Optional<Wallet> lockByPlayerId(@Param("playerId") Long playerId);

    @Query("SELECT w.coins FROM Wallet w WHERE w.playerId = :playerId")
    Optional<Integer> findCoinsByPlayerId(@Param("playerId") Long playerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.coins = w.coins + :amount, w.updatedAt = :now, w.updatedBy = :actorId "
            + "WHERE w.playerId = :playerId")
    int credit(@Param("playerId") Long playerId, @Param("amount") int amount, @Param("now") Instant now,
               @Param("actorId") Long actorId);

    /**
     * Conditional debit (BDY-07): only succeeds when the balance covers the amount.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.coins = w.coins - :amount, w.updatedAt = :now, w.updatedBy = :playerId "
            + "WHERE w.playerId = :playerId AND w.coins >= :amount")
    int debitIfSufficient(@Param("playerId") Long playerId, @Param("amount") int amount, @Param("now") Instant now);
}
