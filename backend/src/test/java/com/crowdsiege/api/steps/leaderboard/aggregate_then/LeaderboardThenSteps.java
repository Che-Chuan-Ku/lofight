package com.crowdsiege.api.steps.leaderboard.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.steps.helpers.PlayerStateAssertions;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class LeaderboardThenSteps {

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    private PlayerStateAssertions playerStateAssertions;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("玩家 {string} 的排行榜成績應為：")
    public void 玩家的排行榜成績應為(String playerName, DataTable dataTable) {
        playerStateAssertions.assertLeaderboard(contextHelper.getPlayerId(playerName), dataTable.asMaps());
    }

    @Then("玩家 {string} 的排行榜成績應為空")
    public void 玩家的排行榜成績應為空(String playerName) {
        assertThat(leaderboardEntryRepository.findByPlayerId(contextHelper.getPlayerId(playerName)))
                .as("玩家 %s 的排行榜成績", playerName).isEmpty();
    }
}
