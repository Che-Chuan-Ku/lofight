package com.crowdsiege.api.cucumber;

import com.crowdsiege.api.service.gateway.NicknameGenerator;

public class StubNicknameGenerator implements NicknameGenerator {

    private static final String DEFAULT_NICKNAME = "勇者000";

    private volatile String nextValue = DEFAULT_NICKNAME;

    public void setNextValue(String nextValue) {
        this.nextValue = nextValue;
    }

    public void reset() {
        this.nextValue = DEFAULT_NICKNAME;
    }

    @Override
    public String next() {
        return nextValue;
    }
}
