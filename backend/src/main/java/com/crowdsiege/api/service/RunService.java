package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.exception.GameErrorCode;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.request.RunCreateRequest;
import com.crowdsiege.api.dto.response.RunDetailResponse;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import com.crowdsiege.api.repository.RunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts a run: tier and loadout checks, conditional stock decrement, board version from settings (D13, D20, D21).
 */
@Service
public class RunService {

    private static final int MAX_SILVER_BUFFS = 1;

    private final RunRepository runRepository;
    private final BuffInventoryRepository buffInventoryRepository;
    private final PlayerService playerService;
    private final GameProperties gameProperties;
    private final ResponseTimes responseTimes;
    private final Clock clock;

    public RunService(RunRepository runRepository, BuffInventoryRepository buffInventoryRepository,
                      PlayerService playerService, GameProperties gameProperties, ResponseTimes responseTimes,
                      Clock clock) {
        this.runRepository = runRepository;
        this.buffInventoryRepository = buffInventoryRepository;
        this.playerService = playerService;
        this.gameProperties = gameProperties;
        this.responseTimes = responseTimes;
        this.clock = clock;
    }

    @Transactional
    public RunDetailResponse createRun(Long playerId, RunCreateRequest request) {
        Tier tier = EnumParameters.required(Tier.class, request.tier(), "tier");
        BuffItem equippedBuff = validateLoadout(tier, request.buffItems());
        if (tier == Tier.SILVER && !playerService.getPlayer(playerId).isSilverUnlocked()) {
            throw new BusinessException(GameErrorCode.SILVER_LOCKED);
        }
        Instant now = clock.instant();
        if (equippedBuff != null && buffInventoryRepository.decrementIfAvailable(playerId, equippedBuff, now) == 0) {
            throw new BusinessException(GameErrorCode.BUFF_OUT_OF_STOCK);
        }
        Run run = runRepository.save(new Run(playerId, tier, equippedBuff, gameProperties.getBoardVersion(), now));
        return new RunDetailResponse(String.valueOf(run.getId()), run.getTier(), run.getEquippedBuff(),
                run.getBoardVersion(), responseTimes.toResponse(run.getStartedAt()), run.getRunState());
    }

    /**
     * Bronze takes no buff (400013); silver takes at most one (400007). Returns the equipped buff or null.
     */
    private static BuffItem validateLoadout(Tier tier, List<String> buffItems) {
        List<BuffItem> buffs = buffItems == null ? List.of()
                : buffItems.stream().map(item -> EnumParameters.required(BuffItem.class, item, "buffItems")).toList();
        if (buffs.isEmpty()) {
            return null;
        }
        if (tier == Tier.BRONZE) {
            throw new BusinessException(CommonErrorCode.BUSINESS_RULE_VIOLATION, "bronze 局不可裝填 buff");
        }
        if (buffs.size() > MAX_SILVER_BUFFS) {
            throw new BusinessException(CommonErrorCode.OUT_OF_RANGE, "參數超出範圍 buffItems");
        }
        return buffs.get(0);
    }
}
