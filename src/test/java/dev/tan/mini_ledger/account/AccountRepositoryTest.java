package dev.tan.mini_ledger.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import java.util.Optional;
import dev.tan.mini_ledger.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountRepositoryTest {

    @Autowired
    AccountRepository accountRepository;

    @Test
    public void test_save_account() {
        accountRepository.save(new Account("VND-001", "VND"));

        Optional<Account> optAccount = accountRepository.findByCode("VND-001");
        assertTrue(optAccount.isPresent());
        assertNotNull(optAccount.get());
        assertEquals("VND-001", optAccount.get().getCode());
        assertEquals("VND", optAccount.get().getCurrency());
        assertNotNull(optAccount.get().getId());
        assertEquals(0, optAccount.get().getBalance());
        assertEquals(0, optAccount.get().getVersion());
    }
}
