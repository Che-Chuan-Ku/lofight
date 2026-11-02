package com.crowdsiege.api.steps.run.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.repository.RunRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;

public class RunThenSteps {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("回應 run 應為：")
    public void 回應run應為(DataTable dataTable) {
        String runId = responseReader.data().path("runId").asText(null);
        assertThat(runId).as("回應中的 runId").isNotBlank();
        assertRun(Long.valueOf(runId), dataTable.asMaps().get(0));
    }

    @Then("run {string} 應為：")
    public void run應為(String runLabel, DataTable dataTable) {
        assertRun(contextHelper.getRunId(runLabel), dataTable.asMaps().get(0));
    }

    @Then("玩家 {string} 的 run 數應為 {int}")
    public void 玩家的run數應為(String playerName, int count) {
        assertThat(runRepository.countByPlayerId(contextHelper.getPlayerId(playerName)))
                .as("玩家 %s 的 run 數", playerName).isEqualTo(count);
    }

    private void assertRun(Long runId, Map<String, String> expectedRow) {
        Run run = runRepository.findById(runId).orElse(null);
        assertThat(run).as("找不到 run id=%s", runId).isNotNull();
        Map<String, Object> actual = new HashMap<>();
        actual.put("tier", run.getTier().name());
        actual.put("equippedBuff", Objects.toString(run.getEquippedBuff(), null));
        actual.put("boardVersion", run.getBoardVersion());
        actual.put("startedAt", run.getStartedAt());
        actual.put("runState", run.getRunState().name());
        actual.put("gameSeconds", run.getGameSeconds());
        actual.put("bossesKilled", run.getBossesKilled());
        actual.put("isVictory", run.getVictory());
        actual.put("deathCause", Objects.toString(run.getDeathCause(), null));
        actual.put("deathBossSeq", run.getDeathBossSeq());
        actual.put("coinsAwarded", run.getCoinsAwarded());
        actual.put("settledAt", run.getSettledAt());
        CellValues.assertRowMatches(expectedRow, actual);
    }
}
