package ca.localcoin.banking.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "banking_transaction", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tx_account_idempotency", columnNames = {"source_account_id", "idempotency_key"})
})
public class BankingTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 36)
    private String reference = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status = TransactionStatus.POSTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "posted_at")
    private Instant postedAt = Instant.now();

    protected BankingTransaction() { }

    public BankingTransaction(TransactionType type, Account source, Account destination,
                              BigDecimal amount, String currency, String description,
                              String idempotencyKey) {
        this.type = type;
        this.sourceAccount = source;
        this.destinationAccount = destination;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Account getSourceAccount() { return sourceAccount; }
    public String getDescription() { return description; }

    public enum TransactionType { TRANSFER, WITHDRAWAL, DEPOSIT, PAYMENT }
    public enum TransactionStatus { PENDING, POSTED, FAILED, REVERSED }
}
