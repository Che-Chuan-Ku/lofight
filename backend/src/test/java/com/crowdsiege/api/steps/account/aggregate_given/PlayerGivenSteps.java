package com.crowdsiege.api.steps.account.aggregate_given;

import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerKind;
import com.crowdsiege.api.model.Wallet;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerGivenSteps {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private MutableClock clock;

    @Given("系統中有以下玩家：")
    public void 系統中有以下玩家(DataTable dataTable) {
        Instant now = clock.instant();
        for (Map<String, String> row : dataTable.asMaps()) {
            Long playerId = playerRepository.nextId();
            Player player = new Player(playerId, PlayerKind.valueOf(row.get("playerKind")), row.get("nickname"),
                    row.get("deviceId"), now);
            if (Boolean.parseBoolean(row.get("isSilverUnlocked"))) {
                player.unlockSilver(now);
            }
            if (Boolean.parseBoolean(row.get("isVoided"))) {
                player.voidInto(null, now);
            }
            playerRepository.save(player);
            walletRepository.save(new Wallet(playerId, 0, now));
            contextHelper.putPlayerId(row.get("玩家"), playerId);
        }
    }
}
