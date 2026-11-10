package kz.nurbol.minibank.transfer;

import kz.nurbol.minibank.account.Account;
import kz.nurbol.minibank.account.AccountRepository;
import kz.nurbol.minibank.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    AccountRepository accounts;
    @Mock
    TransactionRepository transactions;
    @InjectMocks
    TransferService service;

    private static Account account(long id, String currency, String balance) {
        Account a = new Account("KZ" + id, "Owner " + id, currency);
        ReflectionTestUtils.setField(a, "id", id);
        BigDecimal initial = new BigDecimal(balance);
        if (initial.signum() > 0) {
            a.credit(initial);
        }
        return a;
    }

    @Test
    void movesMoneyBetweenAccounts() {
        Account a = account(1, "KZT", "500");
        Account b = account(2, "KZT", "0");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(b));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.transfer(1L, 2L, new BigDecimal("200"), null);

        assertThat(a.getBalance()).isEqualByComparingTo("300");
        assertThat(b.getBalance()).isEqualByComparingTo("200");
    }

    @Test
    void locksAccountsInAscendingIdOrderToAvoidDeadlocks() {
        Account a = account(1, "KZT", "0");
        Account b = account(2, "KZT", "500");
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(a));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(b));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.transfer(2L, 1L, new BigDecimal("100"), null);

        InOrder order = inOrder(accounts);
        order.verify(accounts).findByIdForUpdate(1L);
        order.verify(accounts).findByIdForUpdate(2L);
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalTransaction() {
        Transaction original = Transaction.transfer(1L, 2L, BigDecimal.TEN, "key-1");
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.of(original));

        Transaction result = service.transfer(1L, 2L, BigDecimal.TEN, "key-1");

        assertThat(result).isSameAs(original);
        verifyNoInteractions(accounts);
    }

    @Test
    void rejectsDifferentCurrencies() {
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account(1, "KZT", "500")));
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(account(2, "USD", "0")));

        assertThatThrownBy(() -> service.transfer(1L, 2L, BigDecimal.ONE, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("CURRENCY_MISMATCH");
        verify(transactions, never()).save(any());
    }

    @Test
    void rejectsTransferToSameAccount() {
        assertThatThrownBy(() -> service.transfer(1L, 1L, BigDecimal.ONE, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SAME_ACCOUNT");
    }
}
