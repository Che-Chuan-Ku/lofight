package com.crowdsiege.api.steps.account.aggregate_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.model.RefreshToken;
import com.crowdsiege.api.repository.RefreshTokenRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.TokenHashes;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class RefreshTokenThenSteps {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Then("換發權杖 {string} 應已撤銷，撤銷時間為 {string}")
    public void 換發權杖應已撤銷撤銷時間為(String token, String revokedAt) {
        RefreshToken refreshToken = find(token);
        assertThat(refreshToken.getRevokedAt()).as("換發權杖 %s 撤銷時間", token)
                .isEqualTo(CellValues.instant(revokedAt));
    }

    @Then("換發權杖 {string} 應未撤銷")
    public void 換發權杖應未撤銷(String token) {
        assertThat(find(token).getRevokedAt()).as("換發權杖 %s 撤銷時間", token).isNull();
    }

    private RefreshToken find(String token) {
        return refreshTokenRepository.findByTokenHash(TokenHashes.sha256(token))
                .orElseThrow(() -> new AssertionError("找不到換發權杖 " + token));
    }
}
