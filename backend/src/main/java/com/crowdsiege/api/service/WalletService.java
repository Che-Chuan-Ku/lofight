package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.response.CoinLedgerEntryListResponse;
import com.crowdsiege.api.dto.response.CoinLedgerPageResponse;
import com.crowdsiege.api.dto.response.WalletDetailResponse;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.util.Objects;
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
    private final GameProperties gameProperties;
    private final ResponseTimes responseTimes;

    public WalletService(WalletRepository walletRepository, CoinLedgerEntryRepository coinLedgerEntryRepository,
                         GameProperties gameProperties, ResponseTimes responseTimes) {
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.gameProperties = gameProperties;
        this.responseTimes = responseTimes;
    }

    @Transactional(readOnly = true)
    public WalletDetailResponse getWallet(Long playerId) {
        return new WalletDetailResponse(coinsOf(playerId));
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
                Objects.toString(entry.getRunId(), null), responseTimes.toResponse(entry.getCreatedAt()));
    }
}
