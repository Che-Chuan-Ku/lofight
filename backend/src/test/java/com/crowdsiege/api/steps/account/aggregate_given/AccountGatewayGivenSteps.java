package com.crowdsiege.api.steps.account.aggregate_given;

import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.cucumber.StubAppleIdentityTokenVerifier;
import com.crowdsiege.api.cucumber.StubGoogleIdTokenVerifier;
import com.crowdsiege.api.cucumber.StubNicknameGenerator;
import com.crowdsiege.api.steps.helpers.CellValues;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class AccountGatewayGivenSteps {

    @Autowired
    private StubNicknameGenerator nicknameGenerator;

    @Autowired
    private StubGoogleIdTokenVerifier googleIdTokenVerifier;

    @Autowired
    private StubAppleIdentityTokenVerifier appleIdentityTokenVerifier;

    @Autowired
    private GameProperties gameProperties;

    @Given("系統預設暱稱產生器下一個值為 {string}")
    public void 系統預設暱稱產生器下一個值為(String nickname) {
        nicknameGenerator.setNextValue(nickname);
    }

    @Given("禁用字清單為 {string}")
    public void 禁用字清單為(String words) {
        gameProperties.setBannedWords(new ArrayList<>(CellValues.csv(words)));
    }

    @Given("Google 驗證以下 id_token 的結果為：")
    public void google驗證以下id_token的結果為(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            if (Boolean.parseBoolean(row.get("isValid"))) {
                googleIdTokenVerifier.registerValid(row.get("idToken"), row.get("sub"));
            }
        }
    }

    @Given("Apple 驗證以下 identity token 的結果為：")
    public void apple驗證以下identityToken的結果為(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            if (Boolean.parseBoolean(row.get("isValid"))) {
                appleIdentityTokenVerifier.registerValid(row.get("identityToken"), row.get("sub"));
            }
        }
    }
}
