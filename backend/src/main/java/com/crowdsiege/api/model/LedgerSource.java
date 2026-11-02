package com.crowdsiege.api.model;

public enum LedgerSource implements CodedEnum {
    BOSS_REWARD((short) 0),
    MERGE_IN((short) 1),
    BUFF_PURCHASE((short) 2),
    AD_REWARD((short) 3);

    private final short code;

    LedgerSource(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
