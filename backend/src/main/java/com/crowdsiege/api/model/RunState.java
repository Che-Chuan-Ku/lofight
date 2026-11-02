package com.crowdsiege.api.model;

public enum RunState implements CodedEnum {
    OPEN((short) 0),
    SETTLED((short) 1),
    CHECK_FAILED((short) 2),
    ABANDONED((short) 3);

    private final short code;

    RunState(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
