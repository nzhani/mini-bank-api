package kz.nurbol.minibank.account;

import kz.nurbol.minibank.common.NotFoundException;
import kz.nurbol.minibank.transfer.Transaction;
import kz.nurbol.minibank.transfer.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    // Constructor injection: Spring's IoC container creates and passes the repositories.
    public AccountService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Transactional
    public Account open(String ownerName, String currency) {
        String number;
        do {
            number = generateAccountNumber();
        } while (accounts.existsByAccountNumber(number));
        return accounts.save(new Account(number, ownerName, currency));
    }

    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
    }

    @Transactional
    public Account deposit(Long id, BigDecimal amount) {
        Account account = accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
        account.credit(amount);
        transactions.save(Transaction.deposit(account.getId(), amount));
        return account;
    }

    @Transactional
    public Account block(Long id) {
        Account account = accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
        account.block();
        return account;
    }

    private static String generateAccountNumber() {
        StringBuilder sb = new StringBuilder("KZ");
        for (int i = 0; i < 18; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
