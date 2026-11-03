package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.EconomyErrorCode;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.config.AdRewardProperties;
import com.crowdsiege.api.dto.response.AdRewardDetailResponse;
import com.crowdsiege.api.dto.response.AdRewardStatusResponse;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LedgerSource;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rewarded ad claim (D33-D35): +configured coins after the ad was watched, limited per UTC day and by a cooldown.
 * Daily count and cooldown are derived from the player's own AD_REWARD ledger rows (AMB-A2=b), so a merged guest's
 * claims never carry over (the merge only writes MERGE_IN).
 */
@Service
public class AdRewardService {

    private static final Logger log = LoggerFactory.getLogger(AdRewardService.class);

    private final PlayerService playerService;
    private final WalletRepository walletRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;
    private final PlayerEventRepository playerEventRepository;
    private final AdRewardProperties adRewardProperties;
    private final ResponseTimes responseTimes;
    private final Clock clock;

    public AdRewardService(PlayerService playerService, WalletRepository walletRepository,
                           CoinLedgerEntryRepository coinLedgerEntryRepository,
                           PlayerEventRepository playerEventRepository, AdRewardProperties adRewardProperties,
                           ResponseTimes responseTimes, Clock clock) {
        this.playerService = playerService;
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.playerEventRepository = playerEventRepository;
        this.adRewardProperties = adRewardProperties;
        this.responseTimes = responseTimes;
        this.clock = clock;
    }

    /**
     * Check order (INF-A10): enabled (eco003) -> daily limit (eco001) -> cooldown (eco002). Credit, ledger and event
     * are written in one transaction; claims of one player are serialized by locking the wallet row (INF-A3).
     */
    @Transactional
    public AdRewardDetailResponse claim(Long playerId, String deviceId) {
        if (!adRewardProperties.isEnabled()) {
            throw new BusinessException(EconomyErrorCode.AD_REWARD_NOT_ENABLED);
        }
        Player player = playerService.getPlayer(playerId);
        walletRepository.lockByPlayerId(playerId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for player " + playerId));
        Instant now = clock.instant();
        AdRewardQuota before = quotaOf(playerId, now);
        if (before.remainingCount() == 0) {
            throw new BusinessException(EconomyErrorCode.AD_REWARD_DAILY_LIMIT_REACHED);
        }
        if (before.nextClaimableAt() != null) {
            throw new BusinessException(EconomyErrorCode.AD_REWARD_COOLING_DOWN);
        }
        int coins = adRewardProperties.getCoins();
        walletRepository.credit(playerId, coins, now, playerId);
        int walletCoins = walletRepository.findCoinsByPlayerId(playerId).orElseThrow();
        CoinLedgerEntry entry = coinLedgerEntryRepository.save(
                CoinLedgerEntry.adReward(playerId, deviceId, coins, walletCoins, now));
        playerEventRepository.save(PlayerEvent.adReward(playerId, player.isSilverUnlocked(), deviceId, now));
        AdRewardQuota after = quotaOf(playerId, now);
        log.info("ad_reward_claimed playerId={} coins={} walletCoins={} remainingCount={}", playerId, coins,
                walletCoins, after.remainingCount());
        return new AdRewardDetailResponse(String.valueOf(entry.getId()), coins, walletCoins, after.remainingCount(),
                responseTimes.toResponse(after.nextClaimableAt()));
    }

    /**
     * Shop card state (AMB-A6=a); remaining count and next claimable time are null when the feature is not open.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AdRewardStatusResponse statusOf(Long playerId, Instant now) {
        if (!adRewardProperties.isEnabled()) {
            return new AdRewardStatusResponse(false, null, null);
        }
        AdRewardQuota quota = quotaOf(playerId, now);
        return new AdRewardStatusResponse(true, quota.remainingCount(),
                responseTimes.toResponse(quota.nextClaimableAt()));
    }

    /**
     * INF-A12: remaining = daily limit - today's (UTC) AD_REWARD rows (min 0); next claimable time is null when a
     * claim is possible now, last + cooldown while cooling down, and max(next UTC midnight, last + cooldown) once
     * the daily limit is reached (the cooldown still applies across midnight).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AdRewardQuota quotaOf(Long playerId, Instant now) {
        Instant dayStart = now.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant nextDayStart = dayStart.plus(1, ChronoUnit.DAYS);
        long claimedToday = coinLedgerEntryRepository
                .countByPlayerIdAndLedgerSourceAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(playerId,
                        LedgerSource.AD_REWARD, dayStart, nextDayStart);
        int remainingCount = (int) Math.max(0, adRewardProperties.getDailyLimit() - claimedToday);
        Instant cooldownEnd = coinLedgerEntryRepository
                .findFirstByPlayerIdAndLedgerSourceOrderByCreatedAtDescIdDesc(playerId, LedgerSource.AD_REWARD)
                .map(last -> last.getCreatedAt().plus(adRewardProperties.getCooldown()))
                .orElse(null);
        Instant nextClaimableAt;
        if (remainingCount == 0) {
            nextClaimableAt = cooldownEnd != null && cooldownEnd.isAfter(nextDayStart) ? cooldownEnd : nextDayStart;
        } else if (cooldownEnd != null && now.isBefore(cooldownEnd)) {
            nextClaimableAt = cooldownEnd;
        } else {
            nextClaimableAt = null;
        }
        return new AdRewardQuota(remainingCount, nextClaimableAt);
    }

    public record AdRewardQuota(int remainingCount, Instant nextClaimableAt) {
    }
}
