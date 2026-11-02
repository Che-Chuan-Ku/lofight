package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.PlayerEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerEventRepository extends JpaRepository<PlayerEvent, Long> {

    /**
     * Events in [from, to) - inclusive start, exclusive end (GAP-13 / AMB-04).
     */
    List<PlayerEvent> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}
