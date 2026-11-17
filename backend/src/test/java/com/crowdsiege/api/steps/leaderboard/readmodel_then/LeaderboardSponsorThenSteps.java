package com.crowdsiege.api.steps.leaderboard.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class LeaderboardSponsorThenSteps {

    @Autowired
    private ResponseReader responseReader;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("排行榜 {string} 中玩家 {string} 的 sponsorTier 應為 {int}")
    public void 排行榜中玩家的sponsorTier應為(String field, String playerName, int tier) {
        String playerId = String.valueOf(contextHelper.getPlayerId(playerName));
        JsonNode node = responseReader.data().path(field);
        Iterable<JsonNode> rows = node.isObject() ? java.util.List.of(node) : node;
        for (JsonNode row : rows) {
            if (playerId.equals(row.path("playerId").asText())) {
                assertThat(row.path("sponsorTier").isInt()).as("sponsorTier 必填整數，列 %s", row).isTrue();
                assertThat(row.path("sponsorTier").asInt()).as("玩家 %s sponsorTier", playerName).isEqualTo(tier);
                return;
            }
        }
        throw new AssertionError("排行榜 " + field + " 找不到玩家 " + playerName);
    }

    @Then("排行榜每一列都應有 sponsorTier")
    public void 排行榜每一列都應有sponsorTier() {
        for (JsonNode row : responseReader.data().path("entries")) {
            assertThat(row.path("sponsorTier").isInt()).as("列 %s", row).isTrue();
        }
    }

    @Then("回應原文不應包含 {string}")
    public void 回應原文不應包含(String text) {
        assertThat(scenarioContext.getLastResponse().getBody()).doesNotContain(text);
    }
}
