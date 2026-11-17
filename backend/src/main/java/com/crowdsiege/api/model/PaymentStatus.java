package com.crowdsiege.api.model;

/**
 * Top-up order state; CREATED(0) -> AWAITING_PAYMENT(1, ATM number issued) -> PAID(2) / FAILED(3) / EXPIRED(4).
 */
public enum PaymentStatus implements CodedEnum {
    CREATED((short) 0),
    AWAITING_PAYMENT((short) 1),
    PAID((short) 2),
    FAILED((short) 3),
    EXPIRED((short) 4);

    private final short code;

    PaymentStatus(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
