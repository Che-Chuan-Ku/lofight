package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.exception.GameErrorCode;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.config.GameProperties.TierRule;
import com.crowdsiege.api.dto.request.BossKillRequest;
import com.crowdsiege.api.dto.request.RunSettleRequest;
import com.crowdsiege.api.dto.response.BossRewardResponse;
import com.crowdsiege.api.dto.response.RunSettlementDetailResponse;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.DeathCause;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.model.Run.RunResult;
import com.crowdsiege.api.model.RunBossKill;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.RunBossKillRepository;
import com.crowdsiege.api.repository.RunRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles a run. Order: parameters -> ownership and state -> abandon timeout -> plausibility checks -> award.
 * State changes to ABANDONED / CHECK_FAILED must persist even though an error is returned, hence noRollbackFor.
 */
@Service
public class RunSettlementService {

    private static final Logger log = LoggerFactory.getLogger(RunSettlementService.class);

    private final RunRepository runRepository;
    private final RunBossKillRepository runBossKillRepository;
    private final WalletRepository walletRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;
    private final PlayerRepository playerRepository;
    private final LeaderboardScoreService leaderboardScoreService;
    private final GameProperties gameProperties;
    private final Clock clock;

    public RunSettlementService(RunRepository runRepository, RunBossKillRepository runBossKillRepository,
                                WalletRepository walletRepository, CoinLedgerEntryRepository coinLedgerEntryRepository,
                                PlayerRepository playerRepository, LeaderboardScoreService leaderboardScoreService,
                                GameProperties gameProperties, Clock clock) {
        this.runRepository = runRepository;
        this.runBossKillRepository = runBossKillRepository;
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.playerRepository = playerRepository;
        this.leaderboardScoreService = leaderboardScoreService;
        this.gameProperties = gameProperties;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RunSettlementDetailResponse settle(Long playerId, String runIdParam, RunSettleRequest request) {
        DeathCause deathCause = validateRequest(request);
        Run run = findOwnedRun(playerId, runIdParam);
        Instant now = clock.instant();
        switch (run.getRunState()) {
            case SETTLED -> {
                return toResponse(run);
            }
            case CHECK_FAILED -> throw new BusinessException(GameErrorCode.RUN_RESULT_INVALID);
            case ABANDONED -> throw new BusinessException(GameErrorCode.RUN_ABANDONED);
            case OPEN -> {
            }
        }
        if (Duration.between(run.getStartedAt(), now).compareTo(gameProperties.getAbandonTimeout()) > 0) {
            run.markAbandoned(now);
            throw new BusinessException(GameErrorCode.RUN_ABANDONED);
        }
        if (!isPlausible(run, request, now)) {
            run.markCheckFailed(now);
            log.info("run_check_failed runId={} playerId={}", run.getId(), playerId);
            throw new BusinessException(GameErrorCode.RUN_RESULT_INVALID);
        }
        award(run, request, deathCause, now);
        return toResponse(run);
    }

    /**
     * 400001 for missing gameSeconds / isVictory / bossKills, or a defeat without death cause.
     */
    private static DeathCause validateRequest(RunSettleRequest request) {
        if (request.gameSeconds() == null || request.isVictory() == null || request.bossKills() == null
                || request.bossKills().stream().anyMatch(kill -> kill == null || kill.bossSeq() == null
                || kill.killedAtSecond() == null)) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        if (!request.isVictory() && (request.deathCause() == null || request.deathCause().isBlank())) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 deathCause");
        }
        return request.deathCause() == null || request.deathCause().isBlank() ? null
                : EnumParameters.required(DeathCause.class, request.deathCause(), "deathCause");
    }

    private Run findOwnedRun(Long playerId, String runIdParam) {
        Run run = parseRunId(runIdParam)
                .flatMap(runRepository::findByIdForUpdate)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "指定資源不存在 [runId]"));
        if (!run.getPlayerId().equals(playerId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_ACCESS_DENIED);
        }
        return run;
    }

    private static Optional<Long> parseRunId(String runIdParam) {
        try {
            return Optional.of(Long.valueOf(runIdParam));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * The seven plausibility checks (D17, ASM-03, GAP-03, audit fix 2026-10-30): sequence in range, no duplicates,
     * kill not before spawn, last boss not killed before the one before it, victory at or beyond threshold with all bosses,
     * server elapsed time >= game seconds.
     * D-09 (2026-11-02): victory gameSeconds equal to the threshold is accepted (>=).
     * D39 (2026-11-03): silver has 7 bosses; boss 7 enters after boss 6 dies, so its kill second must be >= the
     * boss 6 kill second, checked only when both are listed (INF-X21).
     * D42 (2026-11-04): bronze boss 9 likewise enters after boss 8 dies, so the relative check now applies to every
     * tier's last boss (taken from the tier's boss count).
     */
    private boolean isPlausible(Run run, RunSettleRequest request, Instant now) {
        TierRule rule = gameProperties.tierRule(run.getTier());
        Set<Integer> seen = new HashSet<>();
        Map<Integer, Integer> killedAt = new HashMap<>();
        for (BossKillRequest kill : request.bossKills()) {
            int bossSeq = kill.bossSeq();
            if (bossSeq < 1 || bossSeq > rule.bossCount() || !seen.add(bossSeq)
                    || kill.killedAtSecond() < rule.spawnSecondOf(bossSeq)) {
                return false;
            }
            killedAt.put(bossSeq, kill.killedAtSecond());
        }
        int lastBossSeq = rule.bossCount();
        if (killedAt.containsKey(lastBossSeq) && killedAt.containsKey(lastBossSeq - 1)
                && killedAt.get(lastBossSeq) < killedAt.get(lastBossSeq - 1)) {
            return false;
        }
        if (request.isVictory() && (request.gameSeconds() < rule.getVictoryThresholdSeconds()
                || seen.size() < rule.bossCount())) {
            return false;
        }
        return Duration.between(run.getStartedAt(), now).toSeconds() >= request.gameSeconds();
    }

    private void award(Run run, RunSettleRequest request, DeathCause deathCause, Instant now) {
        TierRule rule = gameProperties.tierRule(run.getTier());
        List<BossKillRequest> kills = request.bossKills().stream()
                .sorted(Comparator.comparing(BossKillRequest::bossSeq))
                .toList();
        int total = kills.stream().mapToInt(kill -> rule.coinsOf(kill.bossSeq())).sum();
        walletRepository.credit(run.getPlayerId(), total, now, run.getPlayerId());
        int balance = walletRepository.findCoinsByPlayerId(run.getPlayerId()).orElseThrow() - total;
        for (BossKillRequest kill : kills) {
            int coins = rule.coinsOf(kill.bossSeq());
            balance += coins;
            runBossKillRepository.save(new RunBossKill(run.getId(), kill.bossSeq(), kill.killedAtSecond(), coins,
                    run.getPlayerId(), now));
            coinLedgerEntryRepository.save(CoinLedgerEntry.bossReward(run.getPlayerId(), run.getId(), kill.bossSeq(),
                    coins, balance, now));
        }
        boolean silverNewlyUnlocked = unlockSilverIfFirstClear(run, kills, now);
        Run settled = runRepository.findById(run.getId()).orElseThrow();
        settled.markSettled(new RunResult(request.gameSeconds(), request.isVictory(), kills.size(), deathCause,
                request.deathBossSeq()), total, silverNewlyUnlocked, now);
        leaderboardScoreService.recordSettledRun(settled, now);
        log.info("run_settled runId={} playerId={} coinsAwarded={}", run.getId(), run.getPlayerId(), total);
    }

    /**
     * First kill of the bronze unlock boss (9th since D42) unlocks silver permanently (D14).
     */
    private boolean unlockSilverIfFirstClear(Run run, List<BossKillRequest> kills, Instant now) {
        boolean clearedUnlockBoss = run.getTier() == Tier.BRONZE && kills.stream()
                .anyMatch(kill -> kill.bossSeq() == gameProperties.getSilverUnlockBossSeq());
        if (!clearedUnlockBoss) {
            return false;
        }
        Player player = playerRepository.findById(run.getPlayerId()).orElseThrow();
        if (player.isSilverUnlocked()) {
            return false;
        }
        player.unlockSilver(now);
        return true;
    }

    private RunSettlementDetailResponse toResponse(Run run) {
        List<BossRewardResponse> rewards = runBossKillRepository.findByRunIdOrderByBossSeqAsc(run.getId()).stream()
                .map(kill -> new BossRewardResponse(kill.getBossSeq(), kill.getCoinsAwarded()))
                .toList();
        Run current = runRepository.findById(run.getId()).orElseThrow();
        return new RunSettlementDetailResponse(String.valueOf(current.getId()), current.getRunState(),
                current.getCoinsAwarded() == null ? 0 : current.getCoinsAwarded(),
                walletRepository.findCoinsByPlayerId(current.getPlayerId()).orElse(0),
                Boolean.TRUE.equals(current.getSilverNewlyUnlocked()), rewards);
    }
}
