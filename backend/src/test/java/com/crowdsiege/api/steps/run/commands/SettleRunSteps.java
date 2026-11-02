package com.crowdsiege.api.steps.run.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.cucumber.JwtHelper;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ApiClient;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.RequestBodies;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class SettleRunSteps {

    private static final String EMPTY_LIST = "[]";

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private JwtHelper jwtHelper;

    @When("結算 run {string}：")
    public void 結算run(String runLabel, DataTable dataTable) {
        scenarioContext.setLastResponse(settle(runLabel, dataTable.asMaps().get(0),
                scenarioContext.getAccessToken()));
    }

    @Given("玩家 {string} 已結算 run {string}：")
    public void 玩家已結算run(String playerName, String runLabel, DataTable dataTable) {
        String token = jwtHelper.generateToken(contextHelper.getPlayerId(playerName));
        ResponseEntity<String> response = settle(runLabel, dataTable.asMaps().get(0), token);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("前置結算應成功，實際 %s %s", response.getStatusCode(), response.getBody()).isTrue();
    }

    private ResponseEntity<String> settle(String runLabel, Map<String, String> row, String token) {
        String isVictory = row.get("isVictory");
        Map<String, Object> body = RequestBodies.of(
                "gameSeconds", CellValues.integerOrNull(row.get("gameSeconds")),
                "isVictory", isVictory == null ? null : Boolean.valueOf(isVictory),
                "bossKills", bossKills(row.get("bossKills")),
                "deathCause", row.get("deathCause"),
                "deathBossSeq", CellValues.integerOrNull(row.get("deathBossSeq")));
        return apiClient.post("/runs/" + contextHelper.getRunIdOrUnknown(runLabel) + "/actions/settle", body, token);
    }

    /**
     * "bossSeq@killedAtSecond" comma separated; "[]" is an empty list; an empty cell means not provided.
     */
    private List<Map<String, Object>> bossKills(String cell) {
        if (cell == null) {
            return null;
        }
        if (EMPTY_LIST.equals(cell.trim())) {
            return List.of();
        }
        return CellValues.csv(cell).stream()
                .map(kill -> kill.split("@"))
                .map(parts -> Map.<String, Object>of("bossSeq", Integer.valueOf(parts[0]),
                        "killedAtSecond", Integer.valueOf(parts[1])))
                .toList();
    }
}
