package com.crowdsiege.api.steps.ops.aggregate_given;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.EventKind;
import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerEventGivenSteps {

    @Autowired
    private PlayerEventRepository playerEventRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Given("系統中有以下事件：")
    public void 系統中有以下事件(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            String buffItem = row.get("buffItem");
            playerEventRepository.save(new PlayerEvent(EventKind.valueOf(row.get("eventKind")),
                    contextHelper.getPlayerId(row.get("玩家")), Boolean.parseBoolean(row.get("isSilverUnlocked")),
                    buffItem == null ? null : BuffItem.valueOf(buffItem), CellValues.integerOrNull(row.get("paidCoins")),
                    CellValues.instant(row.get("occurredAt"))));
        }
    }
}
