package ca.localcoin.banking.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_balance")
public class AccountBalance {
    // The account ID is also the balance row's primary key: one balance per account.
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance = new BigDecimal("0.00");

    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal ledgerBalance = new BigDecimal("0.00");

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AccountBalance() { }

    public AccountBalance(Account account) {
        this.account = account;
    }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }

    public void debit(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        availableBalance = availableBalance.subtract(amount);
        ledgerBalance = ledgerBalance.subtract(amount);
        updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        availableBalance = availableBalance.add(amount);
        ledgerBalance = ledgerBalance.add(amount);
        updatedAt = Instant.now();
    }
}
