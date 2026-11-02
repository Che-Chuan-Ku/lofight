package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.response.EventStatisticsDetailResponse;
import com.crowdsiege.api.service.EventStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator-only endpoints; no authentication, the /internal/ops/ prefix is blocked at the edge (GAP-13=C).
 */
@RestController
@RequestMapping("${api.prefix}/internal/ops")
public class OpsController {

    private final EventStatisticsService eventStatisticsService;

    public OpsController(EventStatisticsService eventStatisticsService) {
        this.eventStatisticsService = eventStatisticsService;
    }

    @GetMapping("/event-statistics")
    public ManageResponse<EventStatisticsDetailResponse> getEventStatistics(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        return ManageResponse.ok(eventStatisticsService.getStatistics(from, to));
    }
}
