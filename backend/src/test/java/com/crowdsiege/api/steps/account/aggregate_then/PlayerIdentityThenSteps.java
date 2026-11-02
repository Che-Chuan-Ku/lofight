package com.crowdsiege.api.steps.account.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.LoginProvider;
import com.crowdsiege.api.repository.PlayerIdentityRepository;
import com.crowdsiege.api.steps.helpers.PlayerReferenceResolver;
import com.crowdsiege.api.steps.helpers.PlayerStateAssertions;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerIdentityThenSteps {

    @Autowired
    private PlayerIdentityRepository playerIdentityRepository;

    @Autowired
    private PlayerReferenceResolver playerReferenceResolver;

    @Autowired
    private PlayerStateAssertions playerStateAssertions;

    @Then("第三方身分 {word} {string} 對應的帳號應為：")
    public void 第三方身分對應的帳號應為(String provider, String subject, DataTable dataTable) {
        playerStateAssertions.assertPlayer(playerReferenceResolver.accountOf(provider, subject),
                dataTable.asMaps().get(0));
    }

    @Then("第三方身分 {word} {string} 對應的帳號數應為 {int}")
    public void 第三方身分對應的帳號數應為(String provider, String subject, int count) {
        assertThat(playerIdentityRepository.countByLoginProviderAndProviderSubject(LoginProvider.valueOf(provider),
                subject)).as("第三方身分 %s %s 帳號數", provider, subject).isEqualTo(count);
    }

    @Then("第三方身分 {word} {string} 對應帳號的 buff 庫存應為：")
    public void 第三方身分對應帳號的buff庫存應為(String provider, String subject, DataTable dataTable) {
        playerStateAssertions.assertBuffInventory(playerReferenceResolver.accountOf(provider, subject),
                dataTable.asMaps());
    }

    @Then("第三方身分 {word} {string} 對應帳號的排行榜成績應為：")
    public void 第三方身分對應帳號的排行榜成績應為(String provider, String subject, DataTable dataTable) {
        playerStateAssertions.assertLeaderboard(playerReferenceResolver.accountOf(provider, subject),
                dataTable.asMaps());
    }

    @Then("第三方身分 {word} {string} 對應帳號的金幣帳本應依序包含：")
    public void 第三方身分對應帳號的金幣帳本應依序包含(String provider, String subject, DataTable dataTable) {
        playerStateAssertions.assertLedgerStartsWith(playerReferenceResolver.accountOf(provider, subject),
                dataTable.asMaps());
    }
}
