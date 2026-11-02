package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.request.RunCreateRequest;
import com.crowdsiege.api.dto.request.RunSettleRequest;
import com.crowdsiege.api.dto.response.RunDetailResponse;
import com.crowdsiege.api.dto.response.RunSettlementDetailResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.RunService;
import com.crowdsiege.api.service.RunSettlementService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/runs")
public class RunController {

    private final RunService runService;
    private final RunSettlementService runSettlementService;
    private final String apiPrefix;

    public RunController(RunService runService, RunSettlementService runSettlementService,
                         @Value("${api.prefix}") String apiPrefix) {
        this.runService = runService;
        this.runSettlementService = runSettlementService;
        this.apiPrefix = apiPrefix;
    }

    @PostMapping
    public ResponseEntity<ManageResponse<RunDetailResponse>> createRun(HttpServletRequest httpRequest,
                                                                       @RequestBody RunCreateRequest request) {
        RunDetailResponse run = runService.createRun(CurrentUser.getId(httpRequest), request);
        return ResponseEntity.created(URI.create(apiPrefix + "/runs/" + run.runId()))
                .body(ManageResponse.created(run));
    }

    @PostMapping("/{runId}/actions/settle")
    public ManageResponse<RunSettlementDetailResponse> settleRun(HttpServletRequest httpRequest,
                                                                 @PathVariable String runId,
                                                                 @RequestBody RunSettleRequest request) {
        return ManageResponse.ok(runSettlementService.settle(CurrentUser.getId(httpRequest), runId, request));
    }
}
