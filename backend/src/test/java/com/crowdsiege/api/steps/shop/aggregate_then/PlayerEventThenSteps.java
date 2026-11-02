package com.crowdsiege.api.steps.shop.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerEventThenSteps {

    @Autowired
    private PlayerEventRepository playerEventRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("應記錄以下事件：")
    public void 應記錄以下事件(DataTable dataTable) {
        List<PlayerEvent> events = playerEventRepository.findAll();
        List<Map<String, String>> rows = dataTable.asMaps();
        assertThat(events).as("事件筆數").hasSize(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> expected = new HashMap<>(rows.get(i));
            PlayerEvent event = events.get(i);
            assertThat(event.getPlayerId()).as("事件玩家")
                    .isEqualTo(contextHelper.getPlayerId(expected.remove("玩家")));
            Map<String, Object> actual = new HashMap<>();
            actual.put("eventKind", event.getEventKind().name());
            actual.put("isSilverUnlocked", event.isSilverUnlocked());
            actual.put("buffItem", Objects.toString(event.getBuffItem(), null));
            actual.put("paidCoins", event.getPaidCoins());
            actual.put("deviceId", event.getDeviceId());
            actual.put("occurredAt", event.getCreatedAt());
            CellValues.assertRowMatches(expected, actual);
        }
    }

    @Then("事件數應為 {int}")
    public void 事件數應為(int count) {
        assertThat(playerEventRepository.count()).as("事件數").isEqualTo(count);
    }
}
