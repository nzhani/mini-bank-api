package kz.nurbol.minibank.account;

import kz.nurbol.minibank.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private final Account account = new Account("KZ000000000000000001", "Nurbol", "KZT");

    @Test
    void creditAndDebitChangeBalance() {
        account.credit(new BigDecimal("100.00"));
        account.debit(new BigDecimal("30.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("69.50");
    }

    @Test
    void debitMoreThanBalanceIsRejected() {
        account.credit(new BigDecimal("10"));

        assertThatThrownBy(() -> account.debit(new BigDecimal("10.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insufficient funds");
        assertThat(account.getBalance()).isEqualByComparingTo("10");
    }

    @Test
    void nonPositiveAmountIsRejected() {
        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> account.credit(new BigDecimal("-5"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void blockedAccountCannotMoveMoney() {
        account.block();

        assertThatThrownBy(() -> account.credit(BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("blocked");
    }
}
