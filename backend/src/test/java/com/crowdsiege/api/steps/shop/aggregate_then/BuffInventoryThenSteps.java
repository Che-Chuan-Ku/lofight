package com.crowdsiege.api.steps.shop.aggregate_then;

import com.crowdsiege.api.steps.helpers.PlayerStateAssertions;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class BuffInventoryThenSteps {

    @Autowired
    private PlayerStateAssertions playerStateAssertions;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("玩家 {string} 的 buff 庫存應為：")
    public void 玩家的buff庫存應為(String playerName, DataTable dataTable) {
        playerStateAssertions.assertBuffInventory(contextHelper.getPlayerId(playerName), dataTable.asMaps());
    }
}
