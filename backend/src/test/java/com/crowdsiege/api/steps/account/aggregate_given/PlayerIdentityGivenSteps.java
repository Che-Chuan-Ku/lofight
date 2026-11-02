package com.crowdsiege.api.steps.account.aggregate_given;

import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.model.LoginProvider;
import com.crowdsiege.api.model.PlayerIdentity;
import com.crowdsiege.api.repository.PlayerIdentityRepository;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerIdentityGivenSteps {

    @Autowired
    private PlayerIdentityRepository playerIdentityRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private MutableClock clock;

    @Given("玩家 {string} 有以下第三方身分：")
    public void 玩家有以下第三方身分(String playerName, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        for (Map<String, String> row : dataTable.asMaps()) {
            playerIdentityRepository.save(new PlayerIdentity(playerId,
                    LoginProvider.valueOf(row.get("loginProvider")), row.get("providerSubject"), clock.instant()));
        }
    }
}
