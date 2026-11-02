package com.crowdsiege.api.model;

public enum LoginProvider implements CodedEnum {
    GOOGLE((short) 0),
    APPLE((short) 1);

    private final short code;

    LoginProvider(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
