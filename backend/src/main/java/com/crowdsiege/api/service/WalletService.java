package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.response.CoinLedgerEntryListResponse;
import com.crowdsiege.api.dto.response.CoinLedgerPageResponse;
import com.crowdsiege.api.dto.response.LatestSeasonRewardResponse;
import com.crowdsiege.api.dto.response.SeasonRewardItemResponse;
import com.crowdsiege.api.dto.response.WalletDetailResponse;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LedgerSource;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.Season;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wallet balance and own coin ledger (D08).
 */
@Service
public class WalletService {

    private static final int FIRST_PAGE = 1;

    private final WalletRepository walletRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;
    private final PlayerRepository playerRepository;
    private final GameProperties gameProperties;
    private final ResponseTimes responseTimes;

    public WalletService(WalletRepository walletRepository, CoinLedgerEntryRepository coinLedgerEntryRepository,
                         PlayerRepository playerRepository, GameProperties gameProperties,
                         ResponseTimes responseTimes) {
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.playerRepository = playerRepository;
        this.gameProperties = gameProperties;
        this.responseTimes = responseTimes;
    }

    @Transactional(readOnly = true)
    public WalletDetailResponse getWallet(Long playerId) {
        return new WalletDetailResponse(coinsOf(playerId), latestSeasonReward(playerId));
    }

    /**
     * INF-S13: the player's SEASON_REWARD rows of the numerically latest season; null for a guest or when none.
     */
    private LatestSeasonRewardResponse latestSeasonReward(Long playerId) {
        boolean guest = playerRepository.findById(playerId).map(Player::isGuest).orElse(true);
        if (guest) {
            return null;
        }
        Map<Season, List<CoinLedgerEntry>> bySeason = coinLedgerEntryRepository
                .findByPlayerIdAndLedgerSource(playerId, LedgerSource.SEASON_REWARD).stream()
                .filter(entry -> Season.tryParse(entry.getSeasonBoardVersion()).isPresent())
                .collect(Collectors.groupingBy(entry -> Season.of(entry.getSeasonBoardVersion())));
        return bySeason.keySet().stream().max(Comparator.naturalOrder())
                .map(season -> {
                    List<CoinLedgerEntry> entries = bySeason.get(season);
                    Instant awardedAt = entries.stream().map(CoinLedgerEntry::getCreatedAt)
                            .max(Comparator.naturalOrder()).orElseThrow();
                    List<SeasonRewardItemResponse> items = entries.stream()
                            .sorted(Comparator.comparing(CoinLedgerEntry::getSeasonTier))
                            .map(entry -> new SeasonRewardItemResponse(entry.getSeasonTier(), entry.getSeasonRank(),
                                    entry.getCoinDelta()))
                            .toList();
                    return new LatestSeasonRewardResponse(entries.get(0).getSeasonBoardVersion(),
                            responseTimes.toResponse(awardedAt), items);
                })
                .orElse(null);
    }

    public int coinsOf(Long playerId) {
        return walletRepository.findCoinsByPlayerId(playerId).orElse(0);
    }

    /**
     * Newest first, fixed page size; a page past the end returns an empty list (GAP-11).
     */
    @Transactional(readOnly = true)
    public CoinLedgerPageResponse listLedger(Long playerId, String pageParam) {
        int page = parsePage(pageParam);
        int pageSize = gameProperties.getLedgerPageSize();
        Page<CoinLedgerEntry> result = coinLedgerEntryRepository
                .findByPlayerIdOrderByCreatedAtDescIdDesc(playerId, PageRequest.of(page - 1, pageSize));
        return new CoinLedgerPageResponse(result.map(this::toResponse).getContent(), result.getTotalElements(), page,
                result.hasNext());
    }

    /**
     * 400009 when not an integer, 400007 when below 1; absent means page 1.
     */
    private static int parsePage(String pageParam) {
        if (pageParam == null || pageParam.isBlank()) {
            return FIRST_PAGE;
        }
        int page;
        try {
            page = Integer.parseInt(pageParam.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(CommonErrorCode.INVALID_TYPE, "參數型別錯誤 page");
        }
        if (page < FIRST_PAGE) {
            throw new BusinessException(CommonErrorCode.OUT_OF_RANGE, "參數超出範圍 page");
        }
        return page;
    }

    private CoinLedgerEntryListResponse toResponse(CoinLedgerEntry entry) {
        return new CoinLedgerEntryListResponse(String.valueOf(entry.getId()), entry.getLedgerSource(),
                entry.getCoinDelta(), entry.getCoinsAfter(), entry.getBossSeq(), entry.getBuffItem(),
                Objects.toString(entry.getRunId(), null), entry.getSeasonBoardVersion(), entry.getSeasonTier(),
                entry.getSeasonRank(), responseTimes.toResponse(entry.getCreatedAt()));
    }
}
