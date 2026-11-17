package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByMerchantTradeNo(String merchantTradeNo);

    /**
     * Row lock that serializes every notification / query for one order; always taken before the wallet row lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PaymentOrder o WHERE o.merchantTradeNo = :merchantTradeNo")
    Optional<PaymentOrder> lockByMerchantTradeNo(@Param("merchantTradeNo") String merchantTradeNo);

    long countByPlayerIdAndPaymentStatusInAndCreatedAtGreaterThanEqual(
            Long playerId, Collection<PaymentStatus> statuses, Instant from);
}
