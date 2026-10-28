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
import javax.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "bank_account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_person_id", nullable = false)
    private LegalPerson holder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType = AccountType.CURRENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    // Optimistic versioning protects future changes to the account row itself.
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected Account() { }

    public Account(String accountNumber, LegalPerson holder, Branch branch, String currency) {
        this.accountNumber = accountNumber;
        this.holder = holder;
        this.branch = branch;
        this.currency = currency;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public LegalPerson getHolder() { return holder; }
    public Branch getBranch() { return branch; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }

    public enum AccountType { CURRENT, SAVINGS }
    public enum AccountStatus { PENDING, ACTIVE, FROZEN, CLOSED }
}
