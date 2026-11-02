package com.crowdsiege.api.steps.account.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerKind;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.steps.helpers.PlayerStateAssertions;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerThenSteps {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerStateAssertions playerStateAssertions;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Then("玩家 {string} 應為：")
    public void 玩家應為(String playerName, DataTable dataTable) {
        playerStateAssertions.assertPlayer(contextHelper.getPlayerId(playerName), dataTable.asMaps().get(0));
    }

    @Then("裝置識別 {string} 對應的玩家應為：")
    public void 裝置識別對應的玩家應為(String deviceId, DataTable dataTable) {
        Player player = playerRepository.findByDeviceId(deviceId).orElse(null);
        assertThat(player).as("裝置識別 %s 尚無玩家", deviceId).isNotNull();
        playerStateAssertions.assertPlayer(player.getId(), dataTable.asMaps().get(0));
    }

    @Then("系統中裝置識別 {string} 對應的玩家數應為 {int}")
    public void 系統中裝置識別對應的玩家數應為(String deviceId, int count) {
        assertThat(playerRepository.countByDeviceId(deviceId)).as("裝置識別 %s 玩家數", deviceId).isEqualTo(count);
    }

    @Then("系統中帳號（MEMBER）數應為 {int}")
    public void 系統中帳號數應為(int count) {
        assertThat(playerRepository.countByPlayerKind(PlayerKind.MEMBER)).as("MEMBER 帳號數").isEqualTo(count);
    }
}
