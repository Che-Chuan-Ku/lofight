package com.crowdsiege.api.steps.economy.readmodel_then;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdsiege.api.steps.helpers.CellValues;
import com.crowdsiege.api.steps.helpers.ResponseReader;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class CoinLedgerResultThenSteps {

    @Autowired
    private ResponseReader responseReader;

    @Then("回應中 ledgerSource 為 {string} 的項目應為：")
    public void 回應中ledgerSource為的項目應為(String ledgerSource, DataTable dataTable) {
        List<JsonNode> matched = StreamSupport.stream(responseReader.data().path("items").spliterator(), false)
                .filter(item -> ledgerSource.equals(item.path("ledgerSource").asText()))
                .toList();
        List<Map<String, String>> rows = dataTable.asMaps();
        assertThat(matched).as("ledgerSource=%s 的項目筆數", ledgerSource).hasSize(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            CellValues.assertJsonMatches(rows.get(i), matched.get(i));
        }
    }
}
