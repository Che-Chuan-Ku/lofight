package com.crowdsiege.api.service;

import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Plans (merge preview) and applies (login) the one-time merge of a guest into a member account.
 */
@Service
public class GuestMergeService {

    private static final Logger log = LoggerFactory.getLogger(GuestMergeService.class);
    private static final Comparator<LeaderboardEntry> BOARD_KEY_ORDER = Comparator
            .comparing(LeaderboardEntry::getBoardVersion)
            .thenComparing(LeaderboardEntry::getTier);

    private final PlayerRepository playerRepository;
    private final WalletRepository walletRepository;
    private final BuffInventoryRepository buffInventoryRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;

    public GuestMergeService(PlayerRepository playerRepository, WalletRepository walletRepository,
                             BuffInventoryRepository buffInventoryRepository,
                             LeaderboardEntryRepository leaderboardEntryRepository,
                             CoinLedgerEntryRepository coinLedgerEntryRepository) {
        this.playerRepository = playerRepository;
        this.walletRepository = walletRepository;
        this.buffInventoryRepository = buffInventoryRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
    }

    public GuestMergePlan plan(Player guest, Player member) {
        int guestCoins = coinsOf(guest.getId());
        Map<BuffItem, Integer> guestInventory = inventoryOf(guest.getId());
        List<LeaderboardEntry> guestScores = sorted(leaderboardEntryRepository.findByPlayerId(guest.getId()));
        if (member == null) {
            return new GuestMergePlan(guest, null, guestCoins, guestCoins, guest.isSilverUnlocked(), guestInventory,
                    guestInventory, guestScores, guestScores);
        }
        Map<BuffItem, Integer> mergedInventory = inventoryOf(member.getId());
        guestInventory.forEach((buffItem, quantity) -> mergedInventory.merge(buffItem, quantity, Integer::sum));
        return new GuestMergePlan(guest, member, guestCoins, coinsOf(member.getId()) + guestCoins,
                guest.isSilverUnlocked() || member.isSilverUnlocked(), guestInventory, mergedInventory, guestScores,
                mergeBestScores(leaderboardEntryRepository.findByPlayerId(member.getId()), guestScores));
    }

    /**
     * Applies the merge in the caller's transaction and voids the guest (ASM-05: one merge per guest).
     */
    public void apply(Player guest, Player member, Instant now) {
        Long memberId = member.getId();
        int guestCoins = coinsOf(guest.getId());
        walletRepository.credit(memberId, guestCoins, now, memberId);
        walletRepository.credit(guest.getId(), -guestCoins, now, memberId);
        coinLedgerEntryRepository.save(CoinLedgerEntry.mergeIn(memberId, guest.getId(), guestCoins,
                coinsOf(memberId), now));
        inventoryOf(guest.getId()).entrySet().stream().filter(entry -> entry.getValue() > 0).forEach(entry -> {
            BuffItem buffItem = entry.getKey();
            int quantity = entry.getValue();
            buffInventoryRepository.addQuantity(memberId, buffItem.getCode(), quantity, now, memberId);
            buffInventoryRepository.resetQuantity(guest.getId(), buffItem, now, memberId);
        });
        if (guest.isSilverUnlocked() && !member.isSilverUnlocked()) {
            member.unlockSilver(now);
        }
        mergeLeaderboard(guest.getId(), memberId, now);
        guest.voidInto(memberId, now);
        playerRepository.save(member);
        playerRepository.save(guest);
        log.info("guest_merged guestId={} memberId={} coins={}", guest.getId(), memberId, guestCoins);
    }

    private void mergeLeaderboard(Long guestId, Long memberId, Instant now) {
        for (LeaderboardEntry guestEntry : leaderboardEntryRepository.findByPlayerId(guestId)) {
            leaderboardEntryRepository
                    .findByBoardVersionAndTierAndPlayerId(guestEntry.getBoardVersion(), guestEntry.getTier(), memberId)
                    .ifPresentOrElse(memberEntry -> {
                        if (LeaderboardRanking.isStrictlyBetter(guestEntry.isVictory(), guestEntry.getGameSeconds(),
                                memberEntry)) {
                            memberEntry.replaceScore(guestEntry, now, memberId);
                        }
                    }, () -> leaderboardEntryRepository.save(new LeaderboardEntry(memberId,
                            guestEntry.getBoardVersion(), guestEntry.getTier(), guestEntry.getRunId(),
                            guestEntry.isVictory(), guestEntry.getGameSeconds(), guestEntry.getSettledAt(), now)));
            guestEntry.markDeleted(now, memberId);
        }
    }

    private List<LeaderboardEntry> mergeBestScores(List<LeaderboardEntry> memberScores,
                                                   List<LeaderboardEntry> guestScores) {
        Map<String, LeaderboardEntry> best = new LinkedHashMap<>();
        for (LeaderboardEntry entry : memberScores) {
            best.put(boardKey(entry.getBoardVersion(), entry.getTier()), entry);
        }
        for (LeaderboardEntry entry : guestScores) {
            best.merge(boardKey(entry.getBoardVersion(), entry.getTier()), entry,
                    (current, candidate) -> LeaderboardRanking.isStrictlyBetter(candidate.isVictory(),
                            candidate.getGameSeconds(), current) ? candidate : current);
        }
        return sorted(new ArrayList<>(best.values()));
    }

    private static String boardKey(String boardVersion, Tier tier) {
        return boardVersion + "|" + tier;
    }

    private static List<LeaderboardEntry> sorted(List<LeaderboardEntry> entries) {
        return entries.stream().sorted(BOARD_KEY_ORDER).toList();
    }

    private int coinsOf(Long playerId) {
        return walletRepository.findCoinsByPlayerId(playerId).orElse(0);
    }

    private Map<BuffItem, Integer> inventoryOf(Long playerId) {
        Map<BuffItem, Integer> inventory = new EnumMap<>(BuffItem.class);
        for (BuffItem buffItem : BuffItem.values()) {
            inventory.put(buffItem, 0);
        }
        for (BuffInventory row : buffInventoryRepository.findByPlayerId(playerId)) {
            inventory.put(row.getBuffItem(), row.getQuantity());
        }
        return inventory;
    }
}
