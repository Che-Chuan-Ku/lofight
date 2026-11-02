package com.crowdsiege.api.steps.common_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import io.cucumber.java.en.Then;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class CommonThen {

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ResponseReader responseReader;

    @Then("操作成功")
    public void 操作成功() {
        ResponseEntity<String> response = scenarioContext.getLastResponse();
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("預期成功（2XX），實際 %s %s", response.getStatusCode(), response.getBody())
                .isTrue();
    }

    @Then("操作失敗，錯誤代碼為 {string}")
    public void 操作失敗錯誤代碼為(String code) {
        ResponseEntity<String> response = scenarioContext.getLastResponse();
        assertThat(response.getStatusCode().is4xxClientError())
                .as("預期失敗（4XX），實際 %s %s", response.getStatusCode(), response.getBody())
                .isTrue();
        assertThat(responseReader.body().path("code").asText())
                .as("錯誤代碼，回應 %s", response.getBody())
                .isEqualTo(code);
    }

    @Then("錯誤訊息應為 {string}")
    public void 錯誤訊息應為(String message) {
        assertThat(responseReader.body().path("message").asText()).as("錯誤訊息").isEqualTo(message);
    }

    @Then("應有 {int} 筆成功、{int} 筆失敗，失敗錯誤代碼為 {string}")
    public void 應有筆成功筆失敗失敗錯誤代碼為(int successCount, int failureCount, String code) {
        List<ResponseEntity<String>> responses = scenarioContext.getConcurrentResponses();
        List<ResponseEntity<String>> successes = responses.stream()
                .filter(response -> response.getStatusCode().is2xxSuccessful())
                .toList();
        List<ResponseEntity<String>> failures = responses.stream()
                .filter(response -> response.getStatusCode().is4xxClientError())
                .toList();
        assertThat(successes).as("成功筆數，回應 %s", responses).hasSize(successCount);
        assertThat(failures).as("失敗筆數，回應 %s", responses).hasSize(failureCount);
        failures.forEach(failure -> assertThat(responseReader.parse(failure).path("code").asText())
                .as("失敗錯誤代碼").isEqualTo(code));
    }
}
