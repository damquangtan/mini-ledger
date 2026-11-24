package dev.tan.mini_ledger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MiniLedgerApplicationTests {

    @Autowired
    JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

    @Test
    void liquibaseCreatedAccountTable() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'account'",
                Integer.class);
        assertThat(tables).isEqualTo(1);
    }

    @Test
    void liquibaseRecordedChangeset() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM databasechangelog WHERE id = '001-create-account'", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void insertLedgerEntry_WithAccountIdNotExists_ThrowDataIntegrityViolationException() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("INSERT INTO ledger_entry (account_id) VALUES (999)");
        });
    }

    @Test
    void insertTwoJournal_WithSameReference_ThrowDataIntegrityViolationException() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("INSERT INTO journal_transaction (reference) VALUES ('123456')");
            jdbcTemplate.execute("INSERT INTO journal_transaction (reference) VALUES ('123456')");
        });
    }
}
