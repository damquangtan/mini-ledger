package dev.tan.mini_ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

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

}
