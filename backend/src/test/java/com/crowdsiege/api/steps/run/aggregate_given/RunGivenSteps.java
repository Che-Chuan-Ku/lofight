package com.crowdsiege.api.steps.run.aggregate_given;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.model.RunState;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.RunRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class RunGivenSteps {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Given("玩家 {string} 有以下 run：")
    public void 玩家有以下run(String playerName, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        for (Map<String, String> row : dataTable.asMaps()) {
            String equippedBuff = row.get("equippedBuff");
            Instant startedAt = CellValues.instant(row.get("startedAt"));
            Run run = new Run(playerId, Tier.valueOf(row.get("tier")),
                    equippedBuff == null ? null : BuffItem.valueOf(equippedBuff), row.get("boardVersion"), startedAt);
            applyState(run, RunState.valueOf(row.get("runState")), CellValues.integerOrNull(row.get("bossesKilled")),
                    startedAt);
            contextHelper.putRunId(row.get("run"), runRepository.save(run).getId());
        }
    }

    private void applyState(Run run, RunState runState, Integer bossesKilled, Instant startedAt) {
        switch (runState) {
            case OPEN -> {
            }
            case SETTLED -> run.markSettled(new Run.RunResult(0, false,
                    bossesKilled == null ? 0 : bossesKilled, null, null), 0, false, startedAt);
            case CHECK_FAILED -> run.markCheckFailed(startedAt);
            case ABANDONED -> run.markAbandoned(startedAt);
        }
    }
}
