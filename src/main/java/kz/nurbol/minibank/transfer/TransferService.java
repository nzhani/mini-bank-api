package kz.nurbol.minibank.transfer;

import kz.nurbol.minibank.account.Account;
import kz.nurbol.minibank.account.AccountRepository;
import kz.nurbol.minibank.common.BusinessException;
import kz.nurbol.minibank.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public TransferService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    /**
     * Moves money between two accounts atomically.
     * <ul>
     *   <li>Idempotent: repeating a request with the same key returns the original transaction.</li>
     *   <li>Both rows are locked with SELECT ... FOR UPDATE, always in ascending id order,
     *       so two opposite transfers (A to B and B to A) cannot deadlock each other.</li>
     *   <li>If any step fails, @Transactional rolls back the debit, the credit and the record.</li>
     * </ul>
     */
    @Transactional
    public Transaction transfer(Long fromId, Long toId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = transactions.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (fromId.equals(toId)) {
            throw new BusinessException("SAME_ACCOUNT", "Cannot transfer to the same account");
        }

        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Account first = lock(firstId);
        Account second = lock(secondId);
        Account from = first.getId().equals(fromId) ? first : second;
        Account to = from == first ? second : first;

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new BusinessException("CURRENCY_MISMATCH",
                    "Cannot transfer " + from.getCurrency() + " to a " + to.getCurrency() + " account");
        }

        from.debit(amount);
        to.credit(amount);
        return transactions.save(Transaction.transfer(fromId, toId, amount, idempotencyKey));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> history(Long accountId, Pageable pageable) {
        if (!accounts.existsById(accountId)) {
            throw new NotFoundException("Account " + accountId + " not found");
        }
        return transactions.findHistory(accountId, pageable);
    }

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
    }
}
