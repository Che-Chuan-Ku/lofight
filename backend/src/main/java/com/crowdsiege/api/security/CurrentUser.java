package com.crowdsiege.api.security;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads the authenticated player id placed on the request by {@link JwtTokenFilter}.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long getId(HttpServletRequest request) {
        Long playerId = getIdOrNull(request);
        if (playerId == null) {
            throw new BusinessException(CommonErrorCode.TOKEN_MISSING);
        }
        return playerId;
    }

    public static Long getIdOrNull(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtTokenFilter.CURRENT_USER_ID_ATTRIBUTE);
    }
}
