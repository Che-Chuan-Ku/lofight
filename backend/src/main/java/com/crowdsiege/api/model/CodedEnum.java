package com.crowdsiege.api.model;

/**
 * Enum persisted as a stable smallint code (erm.dbml tinyint enums).
 */
public interface CodedEnum {

    short getCode();

    static <E extends Enum<E> & CodedEnum> E fromCode(Class<E> type, short code) {
        for (E constant : type.getEnumConstants()) {
            if (constant.getCode() == code) {
                return constant;
            }
        }
        throw new IllegalStateException("Unknown code " + code + " for " + type.getSimpleName());
    }
}
