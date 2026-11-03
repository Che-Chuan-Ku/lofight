package com.crowdsiege.api.steps.economy.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.LedgerSource;
import com.crowdsiege.api.model.Season;
import com.crowdsiege.api.model.SeasonSettlement;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.SeasonSettlementRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class SeasonSettlementThenSteps {

    private static final Comparator<SeasonSettlement> SETTLEMENT_ORDER = Comparator
            .comparing((SeasonSettlement settlement) -> Season.of(settlement.getBoardVersion()))
            .thenComparing(SeasonSettlement::getTier);

    private static final Comparator<CoinLedgerEntry> AWARD_ORDER = Comparator
            .comparing((CoinLedgerEntry entry) -> Season.of(entry.getSeasonBoardVersion()))
            .thenComparing(CoinLedgerEntry::getSeasonTier)
            .thenComparing(CoinLedgerEntry::getSeasonRank);

    @Autowired
    private SeasonSettlementRepository seasonSettlementRepository;

    @Autowired
    private CoinLedgerEntryRepository coinLedgerEntryRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("賽季獎勵發放記錄應為：")
    public void 賽季獎勵發放記錄應為(DataTable dataTable) {
        List<SeasonSettlement> settlements = sortedSettlements();
        List<Map<String, String>> rows = dataTable.asMaps();
        assertThat(settlements).as("賽季獎勵發放記錄筆數").hasSize(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CellValues.assertRowMatches(rows.get(i), toRow(settlements.get(i)));
        }
    }

    @Then("賽季獎勵發放記錄應包含：")
    public void 賽季獎勵發放記錄應包含(DataTable dataTable) {
        List<SeasonSettlement> settlements = sortedSettlements();
        for (Map<String, String> row : dataTable.asMaps()) {
            SeasonSettlement settlement = settlements.stream()
                    .filter(s -> s.getBoardVersion().equals(row.get("boardVersion"))
                            && s.getTier().name().equals(row.get("tier")))
                    .findFirst()
                    .orElse(null);
            assertThat(settlement).as("找不到 %s %s 發放記錄", row.get("boardVersion"), row.get("tier")).isNotNull();
            CellValues.assertRowMatches(row, toRow(settlement));
        }
    }

    @Then("賽季獎勵發放記錄應為空")
    public void 賽季獎勵發放記錄應為空() {
        assertThat(seasonSettlementRepository.findAll()).as("賽季獎勵發放記錄").isEmpty();
    }

    @Then("應有以下賽季獎勵入帳：")
    public void 應有以下賽季獎勵入帳(DataTable dataTable) {
        List<CoinLedgerEntry> awards = coinLedgerEntryRepository.findAll().stream()
                .filter(entry -> entry.getLedgerSource() == LedgerSource.SEASON_REWARD)
                .sorted(AWARD_ORDER)
                .toList();
        List<Map<String, String>> rows = dataTable.asMaps();
        assertThat(awards).as("賽季獎勵入帳筆數").hasSize(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CoinLedgerEntry entry = awards.get(i);
            Map<String, Object> actual = new HashMap<>();
            actual.put("玩家", contextHelper.playerLabelOf(entry.getPlayerId()));
            actual.put("boardVersion", entry.getSeasonBoardVersion());
            actual.put("tier", entry.getSeasonTier().name());
            actual.put("rank", entry.getSeasonRank());
            actual.put("coinDelta", entry.getCoinDelta());
            CellValues.assertRowMatches(rows.get(i), actual);
        }
    }

    private List<SeasonSettlement> sortedSettlements() {
        return seasonSettlementRepository.findAll().stream().sorted(SETTLEMENT_ORDER).toList();
    }

    private static Map<String, Object> toRow(SeasonSettlement settlement) {
        Map<String, Object> actual = new HashMap<>();
        actual.put("boardVersion", settlement.getBoardVersion());
        actual.put("tier", settlement.getTier().name());
        actual.put("recipientCount", settlement.getRecipientCount());
        actual.put("totalCoins", settlement.getTotalCoins());
        actual.put("settledAt", settlement.getSettledAt());
        return actual;
    }
}
