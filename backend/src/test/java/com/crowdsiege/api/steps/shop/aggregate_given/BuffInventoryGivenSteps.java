package com.crowdsiege.api.steps.shop.aggregate_given;

import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class BuffInventoryGivenSteps {

    @Autowired
    private BuffInventoryRepository buffInventoryRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private MutableClock clock;

    @Given("玩家 {string} 的 buff 庫存為：")
    public void 玩家的buff庫存為(String playerName, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        for (Map<String, String> row : dataTable.asMaps()) {
            buffInventoryRepository.save(new BuffInventory(playerId, BuffItem.valueOf(row.get("buffItem")),
                    Integer.parseInt(row.get("quantity")), clock.instant()));
        }
    }
}
