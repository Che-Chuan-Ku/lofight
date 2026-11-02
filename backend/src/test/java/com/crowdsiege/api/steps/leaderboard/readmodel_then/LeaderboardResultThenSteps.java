package com.crowdsiege.api.steps.leaderboard.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class LeaderboardResultThenSteps {

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("回應中暱稱為 {string} 的 {string} 應有 {int} 筆")
    public void 回應中暱稱為的應有筆(String nickname, String field, int count) {
        assertThat(countMatching(field, row -> nickname.equals(row.path("nickname").asText())))
                .as("暱稱 %s 在 %s 的筆數", nickname, field).isEqualTo(count);
    }

    @Then("回應中玩家 {string} 的 {string} 應有 {int} 筆")
    public void 回應中玩家的應有筆(String playerName, String field, int count) {
        String playerId = String.valueOf(contextHelper.getPlayerId(playerName));
        assertThat(countMatching(field, row -> playerId.equals(row.path("playerId").asText())))
                .as("玩家 %s 在 %s 的筆數", playerName, field).isEqualTo(count);
    }

    private long countMatching(String field, Predicate<JsonNode> predicate) {
        return StreamSupport.stream(responseReader.data().path(field).spliterator(), false)
                .filter(predicate)
                .count();
    }
}
