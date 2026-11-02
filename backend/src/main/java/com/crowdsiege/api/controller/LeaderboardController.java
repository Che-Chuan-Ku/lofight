package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.response.LeaderboardDetailResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.LeaderboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/leaderboards")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ManageResponse<LeaderboardDetailResponse> getLeaderboard(
            HttpServletRequest httpRequest,
            @RequestParam(name = "tier", required = false) String tier,
            @RequestParam(name = "boardVersion", required = false) String boardVersion) {
        return ManageResponse.ok(leaderboardService.getLeaderboard(CurrentUser.getId(httpRequest), tier, boardVersion));
    }
}
