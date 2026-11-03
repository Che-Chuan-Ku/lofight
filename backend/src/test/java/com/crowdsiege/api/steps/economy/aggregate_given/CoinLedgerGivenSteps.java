package com.crowdsiege.api.steps.economy.aggregate_given;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LedgerSource;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class CoinLedgerGivenSteps {

    @Autowired
    private CoinLedgerEntryRepository coinLedgerEntryRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Given("玩家 {string} 有以下金幣帳本：")
    public void 玩家有以下金幣帳本(String playerName, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        for (Map<String, String> row : dataTable.asMaps()) {
            coinLedgerEntryRepository.save(toEntry(playerId, row));
        }
    }

    @Given("玩家 {string} 有 {int} 筆過關入帳帳本，每筆 +1 金幣，時間自 {string} 起每分鐘一筆")
    public void 玩家有筆過關入帳帳本(String playerName, int count, String startAt) {
        Long playerId = contextHelper.getPlayerId(playerName);
        List<CoinLedgerEntry> existing = coinLedgerEntryRepository.findByPlayerIdOrderByCreatedAtDescIdDesc(playerId);
        int coinsAfter = existing.isEmpty() ? 0 : existing.get(0).getCoinsAfter();
        Instant start = CellValues.instant(startAt);
        for (int i = 0; i < count; i++) {
            coinsAfter += 1;
            coinLedgerEntryRepository.save(CoinLedgerEntry.bossReward(playerId, null, 1, 1, coinsAfter,
                    start.plusSeconds(60L * i)));
        }
    }

    private CoinLedgerEntry toEntry(Long playerId, Map<String, String> row) {
        int coinDelta = Integer.parseInt(row.get("coinDelta"));
        int coinsAfter = Integer.parseInt(row.get("coinsAfter"));
        Instant occurredAt = CellValues.instant(row.get("occurredAt"));
        return switch (LedgerSource.valueOf(row.get("ledgerSource"))) {
            case BOSS_REWARD -> CoinLedgerEntry.bossReward(playerId, null,
                    CellValues.integerOrNull(row.get("bossSeq")), coinDelta, coinsAfter, occurredAt);
            case MERGE_IN -> CoinLedgerEntry.mergeIn(playerId, null, coinDelta, coinsAfter, occurredAt);
            case BUFF_PURCHASE -> CoinLedgerEntry.buffPurchase(playerId, BuffItem.valueOf(row.get("buffItem")),
                    -coinDelta, coinsAfter, occurredAt);
            case AD_REWARD -> CoinLedgerEntry.adReward(playerId, row.get("deviceId"), coinDelta, coinsAfter,
                    occurredAt);
            case SEASON_REWARD -> CoinLedgerEntry.seasonReward(playerId, row.get("seasonBoardVersion"),
                    Tier.valueOf(row.get("seasonTier")), Integer.parseInt(row.get("seasonRank")), coinDelta,
                    coinsAfter, occurredAt);
        };
    }
}
