package com.crowdsiege.api.model;

import java.util.List;

public enum BuffItem implements CodedEnum {
    ATTACK_SPEED((short) 0),
    CANNON((short) 1),
    LASER((short) 2),
    // Appended last with a new code so existing persisted codes stay unchanged (D37).
    ATTACK((short) 3);

    /**
     * API listing order (api.yml): ATTACK_SPEED, ATTACK, CANNON, LASER. Do not rely on declaration order.
     */
    public static final List<BuffItem> DISPLAY_ORDER = List.of(ATTACK_SPEED, ATTACK, CANNON, LASER);

    private final short code;

    BuffItem(short code) {
        this.code = code;
    }

    @Override
    public short getCode() {
        return code;
    }
}
