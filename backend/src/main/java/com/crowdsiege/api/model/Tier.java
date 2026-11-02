package com.crowdsiege.api.model;

public enum Tier implements CodedEnum {
    BRONZE((short) 0),
    SILVER((short) 1);

    private final short code;

    Tier(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
