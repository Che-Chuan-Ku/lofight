package com.crowdsiege.api.model;

public enum EventKind implements CodedEnum {
    SHOP_VIEW((short) 0),
    BUFF_PURCHASE((short) 1),
    AD_REWARD((short) 2);

    private final short code;

    EventKind(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
