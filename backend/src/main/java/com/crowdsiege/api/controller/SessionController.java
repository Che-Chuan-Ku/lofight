package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.request.GoogleLoginRequest;
import com.crowdsiege.api.dto.request.GuestSessionCreateRequest;
import com.crowdsiege.api.dto.request.SessionLogoutRequest;
import com.crowdsiege.api.dto.request.SessionRefreshRequest;
import com.crowdsiege.api.dto.response.MergePreviewDetailResponse;
import com.crowdsiege.api.dto.response.SessionDetailResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.MergePreviewService;
import com.crowdsiege.api.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}")
public class SessionController {

    private final SessionService sessionService;
    private final MergePreviewService mergePreviewService;

    public SessionController(SessionService sessionService, MergePreviewService mergePreviewService) {
        this.sessionService = sessionService;
        this.mergePreviewService = mergePreviewService;
    }

    @PostMapping("/sessions/actions/issue-guest")
    public ManageResponse<SessionDetailResponse> issueGuest(@Valid @RequestBody GuestSessionCreateRequest request) {
        return ManageResponse.ok(sessionService.issueGuest(request.deviceId()));
    }

    @PostMapping("/sessions/actions/login-google")
    public ManageResponse<SessionDetailResponse> loginGoogle(HttpServletRequest httpRequest,
                                                             @Valid @RequestBody GoogleLoginRequest request) {
        return ManageResponse.ok(sessionService.loginWithGoogle(request.idToken(), CurrentUser.getIdOrNull(httpRequest)));
    }

    @PostMapping("/sessions/actions/refresh")
    public ManageResponse<SessionDetailResponse> refresh(@Valid @RequestBody SessionRefreshRequest request) {
        return ManageResponse.ok(sessionService.refresh(request.refreshToken()));
    }

    @PostMapping("/sessions/actions/logout")
    public ManageResponse<Map<String, Object>> logout(HttpServletRequest httpRequest,
                                                      @Valid @RequestBody SessionLogoutRequest request) {
        sessionService.logout(CurrentUser.getId(httpRequest), request.refreshToken());
        return ManageResponse.ok(Map.of());
    }

    @GetMapping("/merge-previews")
    public ManageResponse<MergePreviewDetailResponse> getMergePreview(
            HttpServletRequest httpRequest,
            @RequestParam(name = "loginProvider", required = false) String loginProvider,
            @RequestHeader(name = "X-Provider-Token", required = false) String providerToken) {
        return ManageResponse.ok(mergePreviewService.preview(CurrentUser.getId(httpRequest), loginProvider,
                providerToken));
    }
}
