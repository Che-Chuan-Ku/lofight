package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerKind;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query(value = "SELECT nextval('players_id_seq')", nativeQuery = true)
    Long nextId();

    Optional<Player> findByDeviceId(String deviceId);

    long countByDeviceId(String deviceId);

    long countByPlayerKind(PlayerKind playerKind);

    /**
     * Atomic cumulative sponsorship add; the only write path of sponsored_total_twd / first_sponsored_at.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Player p SET p.sponsoredTotalTwd = p.sponsoredTotalTwd + :amount, "
            + "p.firstSponsoredAt = COALESCE(p.firstSponsoredAt, :now), p.updatedAt = :now, p.updatedBy = :playerId "
            + "WHERE p.id = :playerId")
    int addSponsorship(@Param("playerId") Long playerId, @Param("amount") int amount, @Param("now") Instant now);

    @Query("SELECT p.sponsoredTotalTwd FROM Player p WHERE p.id = :playerId")
    Optional<Integer> findSponsoredTotalTwd(@Param("playerId") Long playerId);

    /**
     * Every non-voided player's nickname. Comparison happens in Java with NicknameRules.normalize (NFKC), which SQL
     * cannot reproduce exactly; a full scan is acceptable at the current player count (renames and donations are rare).
     */
    @Query("SELECT new com.crowdsiege.api.repository.NicknameRow(p.id, p.nickname, p.playerKind) FROM Player p "
            + "WHERE p.voided = false")
    List<NicknameRow> findActiveNicknames();
}
