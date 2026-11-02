package com.crowdsiege.api.steps.leaderboard.aggregate_given;

import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerKind;
import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.model.Wallet;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.RunRepository;
import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class LeaderboardGivenSteps {

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private MutableClock clock;

    @Given("系統中有以下排行榜成績：")
    public void 系統中有以下排行榜成績(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            Long playerId = contextHelper.getPlayerId(row.get("玩家"));
            Tier tier = Tier.valueOf(row.get("tier"));
            Instant settledAt = CellValues.instant(row.get("settledAt"));
            Long runId = ensureSettledRun(row.get("run"), playerId, tier, row.get("boardVersion"), settledAt);
            leaderboardEntryRepository.save(new LeaderboardEntry(playerId, row.get("boardVersion"), tier, runId,
                    Boolean.parseBoolean(row.get("isVictory")), Integer.parseInt(row.get("gameSeconds")), settledAt,
                    clock.instant()));
        }
    }

    @Given("版本 {string} {word} 榜有 {int} 位玩家的勝局成績，玩家 {string} 起秒數自 {int} 起每位 +1")
    public void 版本榜有位玩家的勝局成績(String boardVersion, String tierName, int count, String firstPlayer,
                              int firstSeconds) {
        Tier tier = Tier.valueOf(tierName);
        String prefix = firstPlayer.replaceAll("\\d+$", "");
        int firstNumber = Integer.parseInt(firstPlayer.substring(prefix.length()));
        int width = firstPlayer.length() - prefix.length();
        Instant now = clock.instant();
        for (int i = 0; i < count; i++) {
            String name = prefix + String.format("%0" + width + "d", firstNumber + i);
            Long playerId = playerRepository.nextId();
            playerRepository.save(new Player(playerId, PlayerKind.GUEST, name, "dev-" + name, now));
            walletRepository.save(new Wallet(playerId, 0, now));
            contextHelper.putPlayerId(name, playerId);
            Long runId = ensureSettledRun("run-" + name, playerId, tier, boardVersion, now);
            leaderboardEntryRepository.save(new LeaderboardEntry(playerId, boardVersion, tier, runId, true,
                    firstSeconds + i, now, now));
        }
    }

    /**
     * Uses the labelled run when it exists; otherwise creates a SETTLED run for the score.
     */
    private Long ensureSettledRun(String runLabel, Long playerId, Tier tier, String boardVersion, Instant settledAt) {
        if (contextHelper.hasRun(runLabel)) {
            return contextHelper.getRunId(runLabel);
        }
        Run run = new Run(playerId, tier, null, boardVersion, settledAt);
        run.markSettled(new Run.RunResult(0, false, 0, null, null), 0, false, settledAt);
        Long runId = runRepository.save(run).getId();
        contextHelper.putRunId(runLabel, runId);
        return runId;
    }
}
