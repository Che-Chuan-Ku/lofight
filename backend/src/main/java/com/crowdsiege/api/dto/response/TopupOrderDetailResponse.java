package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.PaymentStatus;
import java.time.OffsetDateTime;

/**
 * getTopupOrder data; failure_reason is intentionally not exposed.
 */
public record TopupOrderDetailResponse(String topupOrderId, String packageId, int amountTwd, int coins,
                                       PaymentStatus status, OffsetDateTime paymentExpireAt, OffsetDateTime paidAt,
                                       int walletCoins) {
}
