package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuffInventoryRepository extends JpaRepository<BuffInventory, Long> {

    List<BuffInventory> findByPlayerId(Long playerId);

    Optional<BuffInventory> findByPlayerIdAndBuffItem(Long playerId, BuffItem buffItem);

    /**
     * Conditional decrement (BDY-05): only succeeds while quantity > 0.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE BuffInventory b SET b.quantity = b.quantity - 1, b.updatedAt = :now, b.updatedBy = :playerId "
            + "WHERE b.playerId = :playerId AND b.buffItem = :buffItem AND b.quantity > 0")
    int decrementIfAvailable(@Param("playerId") Long playerId, @Param("buffItem") BuffItem buffItem,
                             @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE BuffInventory b SET b.quantity = 0, b.updatedAt = :now, b.updatedBy = :actorId "
            + "WHERE b.playerId = :playerId AND b.buffItem = :buffItem")
    int resetQuantity(@Param("playerId") Long playerId, @Param("buffItem") BuffItem buffItem,
                      @Param("now") Instant now, @Param("actorId") Long actorId);

    /**
     * Adds quantity, creating the row when absent (upsert on uk_buff_inventories_player_id_buff_item).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "INSERT INTO buff_inventories (player_id, buff_item, quantity, created_at, updated_at, "
            + "created_by, updated_by, is_deleted) "
            + "VALUES (:playerId, :buffItemCode, :amount, :now, :now, :actorId, :actorId, false) "
            + "ON CONFLICT (player_id, buff_item) DO UPDATE "
            + "SET quantity = buff_inventories.quantity + EXCLUDED.quantity, updated_at = EXCLUDED.updated_at, "
            + "updated_by = EXCLUDED.updated_by",
            nativeQuery = true)
    int addQuantity(@Param("playerId") Long playerId, @Param("buffItemCode") short buffItemCode,
                    @Param("amount") int amount, @Param("now") Instant now, @Param("actorId") Long actorId);
}
