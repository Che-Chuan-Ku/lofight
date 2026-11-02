package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.request.PlayerUpdateRequest;
import com.crowdsiege.api.dto.response.PlayerDetailResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.PlayerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/players/me")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public ManageResponse<PlayerDetailResponse> getMyProgress(HttpServletRequest httpRequest) {
        return ManageResponse.ok(playerService.getProgress(CurrentUser.getId(httpRequest)));
    }

    @PatchMapping
    public ManageResponse<PlayerDetailResponse> updateMyNickname(HttpServletRequest httpRequest,
                                                                 @RequestBody PlayerUpdateRequest request) {
        return ManageResponse.ok(playerService.updateNickname(CurrentUser.getId(httpRequest), request.nickname()));
    }
}
