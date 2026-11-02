package com.crowdsiege.api.model;

public enum BuffItem implements CodedEnum {
    ATTACK_SPEED((short) 0),
    CANNON((short) 1),
    LASER((short) 2);

    private final short code;

    BuffItem(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
