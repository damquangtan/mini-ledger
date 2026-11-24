# INC-001: CI fails on a test that passes locally (order-dependent test data)

| Field | Value |
|---|---|
| Date | 2026-11-24 |
| Status | Resolved |
| Severity | Low (CI red, no production impact) |
| Affected | `MiniLedgerApplicationTests.liquibaseCreatedAccountTable` |
| Introduced in | `0769bef` Add test account repository |
| Environments | Local: Windows 11, NTFS (green) · CI: GitHub Actions `ubuntu-latest`, ext4 (red) |

## Summary

After adding `AccountRepositoryTest`, `./mvnw verify` was green on the local machine but red on GitHub Actions. The failing test counted rows in the `account` table and expected `1`. On CI the actual count was `0`.

CI was running the latest commit. The real problem was that the test depended on the order in which test classes run. That order is different on Windows and on Linux.

## Symptoms

CI log:

```
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0 -- in dev.tan.mini_ledger.MiniLedgerApplicationTests
[ERROR] dev.tan.mini_ledger.MiniLedgerApplicationTests.liquibaseCreatedAccountTable <<< FAILURE!
org.opentest4j.AssertionFailedError:
expected: 1
 but was: 0
```

Local run on Windows:

```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The log itself shows that CI ran the latest code: `expected: 1` is the newly committed value. The previous version asserted `isZero()`.

## Root cause

Three things combined to cause the failure.

1. **Shared application context, so a shared database.**
   Both test classes are annotated with `@SpringBootTest` and `@Import(TestcontainersConfiguration.class)`. Their configuration is identical, so the Spring TestContext framework builds one `ApplicationContext`, caches it, and reuses it for both classes.
   The `PostgreSQLContainer` is a bean in that context. Both classes therefore talk to the same PostgreSQL instance.

2. **Committed test data.**
   `AccountRepositoryTest` calls `accountRepository.save(...)`. The test class is not `@Transactional`, so the repository method runs in its own transaction and commits.
   The row `VND-001` stays in the table after the test finishes.

3. **Non-deterministic class order.**
   By default Maven Surefire uses `runOrder=filesystem`: test classes run in the order the operating system lists the compiled classes directory.
   - On Windows (NTFS) that listing is alphabetical, so `account/AccountRepositoryTest` runs before `MiniLedgerApplicationTests`. The count is `1`.
   - On Linux (ext4) the directory order is not alphabetical. On CI, `MiniLedgerApplicationTests` ran first. The count is `0`.

Contributing factor: the original assertion was `isZero()`. It failed locally because of the leftover row, and was changed to `isEqualTo(1)` to make it pass. That hid the problem instead of fixing it.

The test is meant to prove that Liquibase created the `account` table. It should not depend on how many rows the table holds at that moment.

## Resolution

### 1. Make the schema test independent of data

Check that the table exists instead of counting its rows:

```java
@Test
void liquibaseCreatedAccountTable() {
    Integer tables = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM information_schema.tables " +
        "WHERE table_schema = 'public' AND table_name = 'account'",
        Integer.class);
    assertThat(tables).isEqualTo(1);
}
```

### 2. Roll back test data after each test

```java
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AccountRepositoryTest { ... }
```

How this works:

- `TransactionalTestExecutionListener` starts a transaction before each test method and rolls it back afterwards. Rollback is the default.
- Inside the test, `findByCode` still sees the new row. With `GenerationType.IDENTITY` the `INSERT` is executed immediately on `save`. Hibernate also auto-flushes pending changes before running a query in the same transaction.
- After rollback the table is empty again, so every test class starts from a clean database whatever the run order.

### Alternatives considered

| Option | Why not chosen |
|---|---|
| `@DirtiesContext` on the class | Rebuilds the context and starts a new container. Slow, and it treats the symptom only. |
| Delete data in `@AfterEach` | Works, but every new table must be added by hand. Easy to forget. |
| Fix the run order in Surefire (`alphabetical`) | Hides the dependency instead of removing it. |

## Verification

Reproduce the CI failure locally, before the fix:

```bash
./mvnw verify -Dsurefire.runOrder=reversealphabetical
```

After the fix, all of these must be green:

```bash
./mvnw verify -Dsurefire.runOrder=alphabetical
./mvnw verify -Dsurefire.runOrder=reversealphabetical
./mvnw verify -Dsurefire.runOrder=random
```

Run one test class or one method on its own:

```bash
./mvnw test -Dtest=MiniLedgerApplicationTests
./mvnw test -Dtest=AccountRepositoryTest#test_save_account
```

Check the test order Surefire actually used:

```bash
ls target/surefire-reports/
```

Then confirm the GitHub Actions run for the fix commit is green.

## Lessons learned

- If a test passes locally but fails on CI, first confirm which commit CI built. Then look for differences between the two environments: OS, file system, time zone, locale, test order.
- Tests that share a cached Spring context share its database. Every test must clean up after itself, either with `@Transactional` rollback or with explicit cleanup.
- Never change an assertion only to make a test pass. First explain why the actual value is different.
- A schema test should check structure (`information_schema`, `databasechangelog`), not row counts.

## References

- Spring Boot, Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Spring Framework, context caching: https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html
- Spring Framework, transaction management in tests: https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html
- Maven Surefire, `runOrder`: https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html (section runOrder)
