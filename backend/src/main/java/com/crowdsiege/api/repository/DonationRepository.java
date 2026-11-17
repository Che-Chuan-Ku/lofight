package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    boolean existsByTradeNo(String tradeNo);
}
