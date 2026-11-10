package kz.nurbol.minibank.transfer;

import kz.nurbol.minibank.account.Account;
import kz.nurbol.minibank.account.AccountRepository;
import kz.nurbol.minibank.account.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real PostgreSQL in Docker (Testcontainers), including Flyway migrations.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransferIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    AccountService accountService;
    @Autowired
    TransferService transferService;
    @Autowired
    AccountRepository accounts;

    @Test
    void concurrentTransfersNeverLoseOrCreateMoney() throws Exception {
        Account a = accountService.open("Alice", "KZT");
        Account b = accountService.open("Bob", "KZT");
        accountService.deposit(a.getId(), new BigDecimal("1000"));
        accountService.deposit(b.getId(), new BigDecimal("1000"));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            boolean aToB = i % 2 == 0;
            futures.add(pool.submit(() -> transferService.transfer(
                    aToB ? a.getId() : b.getId(), aToB ? b.getId() : a.getId(), BigDecimal.TEN, null)));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        BigDecimal total = accounts.findById(a.getId()).orElseThrow().getBalance()
                .add(accounts.findById(b.getId()).orElseThrow().getBalance());
        assertThat(total).isEqualByComparingTo("2000");
    }

    @Test
    void retriedRequestWithSameKeyIsAppliedOnce() {
        Account a = accountService.open("Carol", "KZT");
        Account b = accountService.open("Dan", "KZT");
        accountService.deposit(a.getId(), new BigDecimal("100"));

        transferService.transfer(a.getId(), b.getId(), new BigDecimal("40"), "retry-key");
        transferService.transfer(a.getId(), b.getId(), new BigDecimal("40"), "retry-key");

        assertThat(accounts.findById(a.getId()).orElseThrow().getBalance()).isEqualByComparingTo("60");
    }
}
