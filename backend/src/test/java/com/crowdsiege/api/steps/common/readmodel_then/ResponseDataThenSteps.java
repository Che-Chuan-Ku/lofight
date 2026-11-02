package com.crowdsiege.api.steps.common.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class ResponseDataThenSteps {

    @Autowired
    private ResponseReader responseReader;

    @Then("回應資料應為：")
    public void 回應資料應為(DataTable dataTable) {
        CellValues.assertJsonMatches(dataTable.asMaps().get(0), responseReader.data());
    }

    @Then("回應中的 {string} 應為：")
    public void 回應中的應為(String field, DataTable dataTable) {
        JsonNode node = responseReader.data().path(field);
        List<Map<String, String>> rows = dataTable.asMaps();
        if (!node.isArray()) {
            assertThat(rows).as("單筆物件只能比對一列").hasSize(1);
            assertThat(node.isObject()).as("回應中的 %s 預期為物件，實際 %s", field, node).isTrue();
            CellValues.assertJsonMatches(rows.get(0), node);
            return;
        }
        assertThat(node.size()).as("回應中的 %s 筆數，實際 %s", field, node).isEqualTo(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CellValues.assertJsonMatches(rows.get(i), node.get(i));
        }
    }

    @Then("回應中的 {string} 應為空")
    public void 回應中的應為空(String field) {
        JsonNode node = responseReader.data().path(field);
        boolean empty = node.isNull() || node.isMissingNode() || (node.isArray() && node.isEmpty());
        assertThat(empty).as("回應中的 %s 預期為空，實際 %s", field, node).isTrue();
    }

    @Then("回應中的 {string} 應有 {int} 筆")
    public void 回應中的應有筆(String field, int count) {
        JsonNode node = responseReader.data().path(field);
        assertThat(node.isArray()).as("回應中的 %s 預期為列表，實際 %s", field, node).isTrue();
        assertThat(node.size()).as("回應中的 %s 筆數", field).isEqualTo(count);
    }

    @Then("回應中的 {string} 第 {int} 筆應為：")
    public void 回應中的第筆應為(String field, int position, DataTable dataTable) {
        JsonNode node = responseReader.data().path(field);
        assertThat(node.size()).as("回應中的 %s 筆數", field).isGreaterThanOrEqualTo(position);
        CellValues.assertJsonMatches(dataTable.asMaps().get(0), node.get(position - 1));
    }
}
