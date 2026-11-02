package com.crowdsiege.api.model;

public enum PlayerKind implements CodedEnum {
    GUEST((short) 0),
    MEMBER((short) 1);

    private final short code;

    PlayerKind(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
