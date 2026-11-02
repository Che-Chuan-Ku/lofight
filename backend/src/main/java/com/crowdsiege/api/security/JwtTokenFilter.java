package com.crowdsiege.api.security;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.exception.ErrorCode;
import com.crowdsiege.api.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Access token check (@cross 驗證存取權杖) for /api/** only: public paths skip it, login paths accept an optional
 * guest token, every other API path requires a valid token. Non-API paths (the bundled front-end page) are not checked.
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ID_ATTRIBUTE = "currentUserId";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_ROOT = "/api/";

    private final AccessTokenService accessTokenService;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final String apiPrefix;
    private final Set<String> publicPaths;
    private final Set<String> optionalAuthPaths;

    public JwtTokenFilter(AccessTokenService accessTokenService, PlayerRepository playerRepository,
                          ObjectMapper objectMapper, @Value("${api.prefix}") String apiPrefix) {
        this.accessTokenService = accessTokenService;
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        this.apiPrefix = apiPrefix;
        this.publicPaths = Set.of(
                apiPrefix + "/sessions/actions/issue-guest",
                apiPrefix + "/sessions/actions/refresh");
        this.optionalAuthPaths = Set.of(
                apiPrefix + "/sessions/actions/login-google",
                apiPrefix + "/sessions/actions/login-apple");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only /api/** is token-protected; the front-end page (/, /index.html) and other static paths are open.
        return !path.startsWith(API_ROOT) || publicPaths.contains(path) || path.startsWith(apiPrefix + "/internal/ops/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean optional = optionalAuthPaths.contains(request.getRequestURI());
        if (header == null || header.isBlank()) {
            if (optional) {
                chain.doFilter(request, response);
            } else {
                writeError(response, CommonErrorCode.TOKEN_MISSING);
            }
            return;
        }
        try {
            request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, authenticate(header));
        } catch (BusinessException ex) {
            writeError(response, ex.getErrorCode());
            return;
        }
        chain.doFilter(request, response);
    }

    private Long authenticate(String header) {
        if (!header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }
        Long playerId = accessTokenService.verify(header.substring(BEARER_PREFIX.length()).trim());
        boolean active = playerRepository.findById(playerId).filter(player -> !player.isVoided()).isPresent();
        if (!active) {
            // Voided (merged) guests and unknown players are rejected (D03).
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }
        return playerId;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ManageResponse.error(errorCode.getCode(), errorCode.getDefaultMessage()));
    }
}
