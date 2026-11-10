package kz.nurbol.minibank.transfer;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Transaction() {
    }

    private Transaction(Long from, Long to, BigDecimal amount, TransactionType type, String key) {
        this.fromAccountId = from;
        this.toAccountId = to;
        this.amount = amount;
        this.type = type;
        this.idempotencyKey = key;
    }

    public static Transaction deposit(Long to, BigDecimal amount) {
        return new Transaction(null, to, amount, TransactionType.DEPOSIT, null);
    }

    public static Transaction transfer(Long from, Long to, BigDecimal amount, String idempotencyKey) {
        return new Transaction(from, to, amount, TransactionType.TRANSFER, idempotencyKey);
    }

    public Long getId() { return id; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
