package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query(value = "SELECT nextval('players_id_seq')", nativeQuery = true)
    Long nextId();

    Optional<Player> findByDeviceId(String deviceId);

    long countByDeviceId(String deviceId);

    long countByPlayerKind(PlayerKind playerKind);
}
