package com.crowdsiege.api.steps.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.WalletRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * States Verify helpers shared by player-name and third-party-identity based Then steps.
 */
@Component
public class PlayerStateAssertions {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BuffInventoryRepository buffInventoryRepository;

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    private CoinLedgerEntryRepository coinLedgerEntryRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private PlayerReferenceResolver playerReferenceResolver;

    public void assertPlayer(Long playerId, Map<String, String> expectedRow) {
        Player player = playerRepository.findById(playerId).orElse(null);
        assertThat(player).as("找不到玩家 id=%s", playerId).isNotNull();
        Map<String, Object> actual = new HashMap<>();
        actual.put("playerKind", player.getPlayerKind().name());
        actual.put("nickname", player.getNickname());
        actual.put("deviceId", player.getDeviceId());
        actual.put("isSilverUnlocked", player.isSilverUnlocked());
        actual.put("isVoided", player.isVoided());
        actual.put("coins", walletRepository.findCoinsByPlayerId(playerId).orElse(null));
        Map<String, String> expected = new HashMap<>(expectedRow);
        String mergedInto = expected.remove("mergedInto");
        CellValues.assertRowMatches(expected, actual);
        if (mergedInto != null) {
            assertThat(player.getMergedIntoPlayerId()).as("mergedInto")
                    .isEqualTo(playerReferenceResolver.resolve(mergedInto));
        }
    }

    public void assertBuffInventory(Long playerId, List<Map<String, String>> expectedRows) {
        Map<BuffItem, Integer> quantities = new HashMap<>();
        for (BuffInventory inventory : buffInventoryRepository.findByPlayerId(playerId)) {
            quantities.put(inventory.getBuffItem(), inventory.getQuantity());
        }
        for (Map<String, String> row : expectedRows) {
            BuffItem buffItem = BuffItem.valueOf(row.get("buffItem"));
            assertThat(quantities.getOrDefault(buffItem, 0)).as("buff %s 數量", buffItem)
                    .isEqualTo(Integer.parseInt(row.get("quantity")));
        }
    }

    public void assertLeaderboard(Long playerId, List<Map<String, String>> expectedRows) {
        List<LeaderboardEntry> entries = leaderboardEntryRepository.findByPlayerId(playerId);
        assertThat(entries).as("排行榜成績筆數").hasSize(expectedRows.size());
        for (Map<String, String> row : expectedRows) {
            LeaderboardEntry entry = entries.stream()
                    .filter(e -> e.getBoardVersion().equals(row.get("boardVersion"))
                            && e.getTier().name().equals(row.get("tier")))
                    .findFirst()
                    .orElse(null);
            assertThat(entry).as("找不到 %s %s 成績", row.get("boardVersion"), row.get("tier")).isNotNull();
            Map<String, Object> actual = new HashMap<>();
            actual.put("boardVersion", entry.getBoardVersion());
            actual.put("tier", entry.getTier().name());
            actual.put("run", contextHelper.runLabelOf(entry.getRunId()));
            actual.put("isVictory", entry.isVictory());
            actual.put("gameSeconds", entry.getGameSeconds());
            CellValues.assertRowMatches(row, actual);
        }
    }

    public void assertLedgerStartsWith(Long playerId, List<Map<String, String>> expectedRows) {
        List<CoinLedgerEntry> entries = coinLedgerEntryRepository.findByPlayerIdOrderByCreatedAtDescIdDesc(playerId);
        assertThat(entries.size()).as("帳本筆數").isGreaterThanOrEqualTo(expectedRows.size());
        for (int i = 0; i < expectedRows.size(); i++) {
            CoinLedgerEntry entry = entries.get(i);
            Map<String, Object> actual = new HashMap<>();
            actual.put("ledgerSource", entry.getLedgerSource().name());
            actual.put("coinDelta", entry.getCoinDelta());
            actual.put("coinsAfter", entry.getCoinsAfter());
            actual.put("bossSeq", entry.getBossSeq());
            actual.put("buffItem", Objects.toString(entry.getBuffItem(), null));
            CellValues.assertRowMatches(expectedRows.get(i), actual);
        }
    }
}
