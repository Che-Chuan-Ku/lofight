package com.crowdsiege.api.model;

public enum DeathCause implements CodedEnum {
    RED_BREACH((short) 0),
    BOSS_REACHED((short) 1);

    private final short code;

    DeathCause(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
