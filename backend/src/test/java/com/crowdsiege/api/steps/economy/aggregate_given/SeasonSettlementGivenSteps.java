package com.crowdsiege.api.steps.economy.aggregate_given;

import com.crowdsiege.api.model.SeasonSettlement;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.SeasonSettlementRepository;
import com.crowdsiege.api.steps.helpers.CellValues;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class SeasonSettlementGivenSteps {

    /**
     * Test-only fault injection: a trigger rejects SEASON_REWARD ledger inserts of the listed (season, tier).
     */
    public static final String FAILURE_TRIGGER_CLEANUP = "DROP TRIGGER IF EXISTS test_season_reward_failure "
            + "ON coin_ledger_entries; DROP TABLE IF EXISTS test_season_reward_failures";

    @Autowired
    private SeasonSettlementRepository seasonSettlementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Given("系統中有以下賽季獎勵發放記錄：")
    public void 系統中有以下賽季獎勵發放記錄(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            seasonSettlementRepository.save(new SeasonSettlement(row.get("boardVersion"),
                    Tier.valueOf(row.get("tier")), Integer.parseInt(row.get("recipientCount")),
                    Integer.parseInt(row.get("totalCoins")), CellValues.instant(row.get("settledAt"))));
        }
    }

    @Given("賽季 {string} 的 {string} 榜入帳會失敗")
    public void 賽季的榜入帳會失敗(String boardVersion, String tierName) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test_season_reward_failures "
                + "(board_version varchar(16) NOT NULL, tier smallint NOT NULL)");
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION test_season_reward_failure() RETURNS trigger AS $$
                BEGIN
                    IF EXISTS (SELECT 1 FROM test_season_reward_failures f
                               WHERE f.board_version = NEW.season_board_version AND f.tier = NEW.season_tier) THEN
                        RAISE EXCEPTION 'injected season reward failure %/%', NEW.season_board_version,
                            NEW.season_tier;
                    END IF;
                    RETURN NEW;
                END
                $$ LANGUAGE plpgsql""");
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS test_season_reward_failure ON coin_ledger_entries");
        jdbcTemplate.execute("CREATE TRIGGER test_season_reward_failure BEFORE INSERT ON coin_ledger_entries "
                + "FOR EACH ROW EXECUTE FUNCTION test_season_reward_failure()");
        jdbcTemplate.update("INSERT INTO test_season_reward_failures (board_version, tier) VALUES (?, ?)",
                boardVersion, Tier.valueOf(tierName).getCode());
    }

    @Given("賽季 {string} 的 {string} 榜入帳不再失敗")
    public void 賽季的榜入帳不再失敗(String boardVersion, String tierName) {
        jdbcTemplate.update("DELETE FROM test_season_reward_failures WHERE board_version = ? AND tier = ?",
                boardVersion, Tier.valueOf(tierName).getCode());
    }
}
