package com.crowdsiege.api.steps.helpers;

import com.crowdsiege.api.cucumber.ScenarioContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Natural key lookups: player names and run labels used in Gherkin map to database ids.
 */
@Component
public class ScenarioContextHelper {

    private static final String PLAYER_KEY = "player:";
    private static final String RUN_KEY = "run:";
    private static final long UNKNOWN_RUN_ID = 999_999_999L;

    @Autowired
    private ScenarioContext scenarioContext;

    public void putPlayerId(String playerName, Long playerId) {
        scenarioContext.putId(PLAYER_KEY + playerName, playerId);
    }

    public Long getPlayerId(String playerName) {
        Long playerId = scenarioContext.getId(PLAYER_KEY + playerName);
        if (playerId == null) {
            throw new IllegalStateException("找不到玩家 '" + playerName + "' 的 ID，請先建立玩家");
        }
        return playerId;
    }

    public void putRunId(String runLabel, Long runId) {
        scenarioContext.putId(RUN_KEY + runLabel, runId);
    }

    public boolean hasRun(String runLabel) {
        return scenarioContext.hasId(RUN_KEY + runLabel);
    }

    public Long getRunId(String runLabel) {
        Long runId = scenarioContext.getId(RUN_KEY + runLabel);
        if (runId == null) {
            throw new IllegalStateException("找不到 run '" + runLabel + "' 的 ID");
        }
        return runId;
    }

    /**
     * Id for a run label; labels never created map to an id that does not exist.
     */
    public Long getRunIdOrUnknown(String runLabel) {
        Long runId = scenarioContext.getId(RUN_KEY + runLabel);
        return runId == null ? UNKNOWN_RUN_ID : runId;
    }

    public String runLabelOf(Long runId) {
        return scenarioContext.labelOf(RUN_KEY, runId);
    }
}
