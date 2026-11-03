package com.crowdsiege.api.steps.economy.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class LatestSeasonRewardThenSteps {

    @Autowired
    private ResponseReader responseReader;

    @Then("回應的最近一次賽季獎勵應為：")
    public void 回應的最近一次賽季獎勵應為(DataTable dataTable) {
        JsonNode reward = responseReader.data().path("latestSeasonReward");
        assertThat(reward.isObject()).as("latestSeasonReward 應為物件，實際 %s", reward).isTrue();
        assertThat(reward.path("awardedAt").asText()).as("awardedAt").isNotBlank();
        JsonNode items = reward.path("items");
        List<Map<String, String>> rows = dataTable.asMaps();
        assertThat(items.size()).as("最近一次賽季獎勵筆數").isEqualTo(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> actual = new HashMap<>();
            actual.put("boardVersion", reward.path("boardVersion").asText());
            actual.put("tier", CellValues.jsonValue(items.get(i).get("tier")));
            actual.put("rank", CellValues.jsonValue(items.get(i).get("rank")));
            actual.put("coins", CellValues.jsonValue(items.get(i).get("coins")));
            CellValues.assertRowMatches(rows.get(i), actual);
        }
    }

    @Then("回應的最近一次賽季獎勵應為空")
    public void 回應的最近一次賽季獎勵應為空() {
        JsonNode data = responseReader.data();
        assertThat(data.has("latestSeasonReward")).as("latestSeasonReward 應明確序列化").isTrue();
        assertThat(data.get("latestSeasonReward").isNull()).as("latestSeasonReward 應為 null").isTrue();
    }
}
