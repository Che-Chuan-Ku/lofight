package com.crowdsiege.api.steps.account.aggregate_given;

import com.crowdsiege.api.cucumber.MutableClock;
import com.crowdsiege.api.model.RefreshToken;
import com.crowdsiege.api.repository.RefreshTokenRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ScenarioContextHelper;
import com.crowdsiege.api.steps.helpers.TokenHashes;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class RefreshTokenGivenSteps {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Autowired
    private MutableClock clock;

    @Given("玩家 {string} 有以下換發權杖：")
    public void 玩家有以下換發權杖(String playerName, DataTable dataTable) {
        Long playerId = contextHelper.getPlayerId(playerName);
        for (Map<String, String> row : dataTable.asMaps()) {
            RefreshToken token = new RefreshToken(playerId, TokenHashes.sha256(row.get("token")),
                    CellValues.instant(row.get("expiresAt")), clock.instant());
            if (row.get("revokedAt") != null) {
                token.revoke(CellValues.instant(row.get("revokedAt")), null);
            }
            refreshTokenRepository.save(token);
        }
    }
}
