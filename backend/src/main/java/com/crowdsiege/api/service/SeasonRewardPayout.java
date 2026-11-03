package com.crowdsiege.api.service;

import com.crowdsiege.api.config.SeasonRewardProperties;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.model.PlayerKind;
import com.crowdsiege.api.model.SeasonSettlement;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.SeasonSettlementRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pays one (season, tier) board in its own transaction (INF-S14): settlement record, wallet credits, SEASON_REWARD
 * ledger rows and events commit or roll back together.
 */
@Service
public class SeasonRewardPayout {

    private static final Logger log = LoggerFactory.getLogger(SeasonRewardPayout.class);

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final PlayerRepository playerRepository;
    private final WalletRepository walletRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;
    private final PlayerEventRepository playerEventRepository;
    private final SeasonSettlementRepository seasonSettlementRepository;
    private final SeasonRewardProperties seasonRewardProperties;
    private final Clock clock;

    public SeasonRewardPayout(LeaderboardEntryRepository leaderboardEntryRepository,
                              PlayerRepository playerRepository, WalletRepository walletRepository,
                              CoinLedgerEntryRepository coinLedgerEntryRepository,
                              PlayerEventRepository playerEventRepository,
                              SeasonSettlementRepository seasonSettlementRepository,
                              SeasonRewardProperties seasonRewardProperties, Clock clock) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.playerRepository = playerRepository;
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.playerEventRepository = playerEventRepository;
        this.seasonSettlementRepository = seasonSettlementRepository;
        this.seasonRewardProperties = seasonRewardProperties;
        this.clock = clock;
    }

    /**
     * Ranks the board as the leaderboard query does (GAP-12, INF-S10), takes the paid ranks and credits only
     * MEMBER, non-voided players at payout time; guest ranks are skipped, not backfilled (AMB-S4=a).
     * The settlement row is inserted first, so a concurrent instance blocks on its unique key and then fails
     * instead of paying twice (INF-S4).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SeasonSettlement pay(String boardVersion, Tier tier) {
        Instant now = clock.instant();
        List<Award> awards = awardsOf(boardVersion, tier);
        int totalCoins = awards.stream().mapToInt(Award::coins).sum();
        SeasonSettlement settlement = seasonSettlementRepository.saveAndFlush(
                new SeasonSettlement(boardVersion, tier, awards.size(), totalCoins, now));
        // Lock wallets in a fixed (player id) order so two boards paid concurrently cannot deadlock.
        awards.stream().map(award -> award.player().getId()).sorted().forEach(playerId ->
                walletRepository.lockByPlayerId(playerId).orElseThrow(() -> new IllegalStateException(
                        "Wallet not found for player " + playerId)));
        for (Award award : awards) {
            Long playerId = award.player().getId();
            walletRepository.credit(playerId, award.coins(), now, playerId);
            int coinsAfter = walletRepository.findCoinsByPlayerId(playerId).orElseThrow();
            coinLedgerEntryRepository.saveAndFlush(CoinLedgerEntry.seasonReward(playerId, boardVersion, tier,
                    award.rank(), award.coins(), coinsAfter, now));
            playerEventRepository.save(PlayerEvent.seasonReward(playerId, award.player().isSilverUnlocked(), now));
        }
        log.info("season_reward_paid boardVersion={} tier={} recipientCount={} totalCoins={}", boardVersion, tier,
                awards.size(), totalCoins);
        return settlement;
    }

    private List<Award> awardsOf(String boardVersion, Tier tier) {
        List<LeaderboardEntry> top = leaderboardEntryRepository.findByBoardVersionAndTier(boardVersion, tier)
                .stream()
                .sorted(LeaderboardRanking.BOARD_ORDER)
                .limit(seasonRewardProperties.paidRanks())
                .toList();
        Map<Long, Player> players = playerRepository
                .findAllById(top.stream().map(LeaderboardEntry::getPlayerId).toList()).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        List<Award> awards = new ArrayList<>();
        for (int index = 0; index < top.size(); index++) {
            Player player = players.get(top.get(index).getPlayerId());
            if (isEligible(player)) {
                int rank = index + 1;
                awards.add(new Award(player, rank, seasonRewardProperties.coinsOfRank(rank)));
            }
        }
        awards.sort(Comparator.comparingInt(Award::rank));
        return awards;
    }

    /**
     * INF-S2: identity at payout time; a guest bound later is not paid afterwards (AMB-S8=a).
     */
    private static boolean isEligible(Player player) {
        return player != null && player.getPlayerKind() == PlayerKind.MEMBER && !player.isVoided();
    }

    private record Award(Player player, int rank, int coins) {
    }
}
