package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import java.util.Arrays;

/**
 * Parses enum request parameters: 400001 when missing, 400012 when not an allowed value.
 */
public final class EnumParameters {

    private EnumParameters() {
    }

    public static <E extends Enum<E>> E required(Class<E> type, String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 " + parameterName);
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_ENUM,
                        "參數值不在允許清單 " + parameterName));
    }
}
