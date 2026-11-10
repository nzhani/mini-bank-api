package kz.nurbol.minibank.account;

import jakarta.persistence.*;
import kz.nurbol.minibank.common.BusinessException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Version
    private long version;

    protected Account() {
        // required by JPA
    }

    public Account(String accountNumber, String ownerName, String currency) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
    }

    /** Business rules live in the entity, so they cannot be bypassed by a service. */
    public void credit(BigDecimal amount) {
        requireActive();
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requireActive();
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_FUNDS",
                    "Account " + accountNumber + " has insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    public void block() {
        status = AccountStatus.BLOCKED;
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_BLOCKED", "Account " + accountNumber + " is blocked");
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Amount must be positive");
        }
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
