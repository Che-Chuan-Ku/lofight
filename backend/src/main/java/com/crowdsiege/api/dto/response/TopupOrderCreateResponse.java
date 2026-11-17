package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.PaymentStatus;

public record TopupOrderCreateResponse(String topupOrderId, String packageId, int amountTwd, int coins,
                                       PaymentStatus status, TopupCheckoutResponse checkout) {
}
