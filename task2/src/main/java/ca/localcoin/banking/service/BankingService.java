package ca.localcoin.banking.service;

import ca.localcoin.banking.api.ApiDtos.AccountResponse;
import ca.localcoin.banking.api.ApiDtos.MoneyRequest;
import ca.localcoin.banking.api.ApiDtos.MoneyResponse;
import ca.localcoin.banking.api.ApiDtos.OpenAccountRequest;
import ca.localcoin.banking.api.ApiDtos.OpenAccountResponse;
import ca.localcoin.banking.domain.Account;
import ca.localcoin.banking.domain.AccountBalance;
import ca.localcoin.banking.domain.AuditEvent;
import ca.localcoin.banking.domain.BankingTransaction;
import ca.localcoin.banking.domain.Branch;
import ca.localcoin.banking.domain.LedgerEntry;
import ca.localcoin.banking.domain.LegalPerson;
import ca.localcoin.banking.domain.LegalPersonAddress;
import ca.localcoin.banking.repository.AccountBalanceRepository;
import ca.localcoin.banking.repository.AccountRepository;
import ca.localcoin.banking.repository.AuditEventRepository;
import ca.localcoin.banking.repository.BankingTransactionRepository;
import ca.localcoin.banking.repository.BranchRepository;
import ca.localcoin.banking.repository.LedgerEntryRepository;
import ca.localcoin.banking.repository.LegalPersonAddressRepository;
import ca.localcoin.banking.repository.LegalPersonRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankingService {
    private final BranchRepository branches;
    private final LegalPersonRepository people;
    private final LegalPersonAddressRepository addresses;
    private final AccountRepository accounts;
    private final AccountBalanceRepository balances;
    private final BankingTransactionRepository transactions;
    private final LedgerEntryRepository ledgerEntries;
    private final AuditEventRepository auditEvents;
    private final CurrencyPolicy currencyPolicy;
    private final SecureRandom random = new SecureRandom();

    public BankingService(BranchRepository branches, LegalPersonRepository people,
                          LegalPersonAddressRepository addresses, AccountRepository accounts,
                          AccountBalanceRepository balances, BankingTransactionRepository transactions,
                          LedgerEntryRepository ledgerEntries, AuditEventRepository auditEvents,
                          CurrencyPolicy currencyPolicy) {
        this.branches = branches;
        this.people = people;
        this.addresses = addresses;
        this.accounts = accounts;
        this.balances = balances;
        this.transactions = transactions;
        this.ledgerEntries = ledgerEntries;
        this.auditEvents = auditEvents;
        this.currencyPolicy = currencyPolicy;
    }

    @Transactional
    public OpenAccountResponse openAccount(OpenAccountRequest request) {
        String currency = currencyPolicy.requireSupported(request.currency);
        Branch branch = branches.findById(request.branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (branch.getStatus() != Branch.BranchStatus.ACTIVE) {
            throw new BusinessRuleException("Branch is not active");
        }

        LegalPerson person = people.save(new LegalPerson(
                request.firstName.trim(), blankToNull(request.middleName), request.lastName.trim(),
                request.dateOfBirth, blankToNull(request.email), blankToNull(request.phone)));

        String countryCode = request.countryCode.trim().toUpperCase(Locale.ROOT);
        addresses.save(new LegalPersonAddress(person, request.addressLine1.trim(),
                blankToNull(request.addressLine2), request.city.trim(), blankToNull(request.provinceState),
                request.postalCode.trim(), countryCode));

        Account account = accounts.save(new Account(createAccountNumber(), person, branch, currency));
        balances.save(new AccountBalance(account));
        auditEvents.save(new AuditEvent("ACCOUNT_OPENED", "ACCOUNT", account.getId().toString(),
                AuditEvent.Outcome.SUCCESS, UUID.randomUUID().toString(), "branchId=" + branch.getId()));

        return new OpenAccountResponse(account.getId(), account.getAccountNumber(), account.getStatus().name());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        AccountBalance balance = balances.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account balance is missing"));
        LegalPerson person = account.getHolder();

        StringBuilder holderName = new StringBuilder(person.getFirstName());
        if (person.getMiddleName() != null && !person.getMiddleName().trim().isEmpty()) {
            holderName.append(' ').append(person.getMiddleName().trim());
        }
        holderName.append(' ').append(person.getLastName());

        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getStatus().name(),
                account.getCurrency(), account.getBranch().getId(), holderName.toString(),
                person.getDateOfBirth(), person.getEmail(), balance.getAvailableBalance(), balance.getLedgerBalance());
    }

    @Transactional
    public MoneyResponse withdraw(Long accountId, MoneyRequest request) {
        String currency = currencyPolicy.requireSupported(request.currency);
        BigDecimal amount = normalizeAmount(request.amount);
        String key = request.idempotencyKey.trim();
        String description = blankToNull(request.description);

        // A retry of the same request should return the first transaction, not debit again.
        Optional<BankingTransaction> previous = transactions.findBySourceAccount_IdAndIdempotencyKey(accountId, key);
        if (previous.isPresent()) {
            return replayOrReject(previous.get(), accountId, amount, currency, description);
        }

        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active");
        }
        if (!account.getCurrency().equals(currency)) {
            throw new BusinessRuleException("Request currency does not match account currency");
        }

        // Lock the balance before checking funds so two withdrawals cannot spend the same money.
        AccountBalance balance = balances.findLockedByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("Account balance is missing"));

        // Check again after acquiring the lock in case the same request arrived concurrently.
        previous = transactions.findLockedByAccountAndKey(accountId, key);
        if (previous.isPresent()) {
            return replayOrReject(previous.get(), accountId, amount, currency, description);
        }

        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds");
        }
        balance.debit(amount);

        BankingTransaction transaction = transactions.save(new BankingTransaction(
                BankingTransaction.TransactionType.WITHDRAWAL, account, null,
                amount, currency, description, key));
        ledgerEntries.save(new LedgerEntry(transaction, account, LedgerEntry.EntryType.DEBIT,
                amount, currency, balance.getLedgerBalance()));
        auditEvents.save(new AuditEvent("WITHDRAWAL_POSTED", "ACCOUNT", accountId.toString(),
                AuditEvent.Outcome.SUCCESS, key, "transactionId=" + transaction.getId()));

        return toResponse(transaction, false);
    }

    private MoneyResponse replayOrReject(BankingTransaction previous, Long accountId,
                                         BigDecimal amount, String currency, String description) {
        boolean sameRequest = previous.getType() == BankingTransaction.TransactionType.WITHDRAWAL
                && previous.getSourceAccount() != null
                && previous.getSourceAccount().getId().equals(accountId)
                && previous.getAmount().compareTo(amount) == 0
                && previous.getCurrency().equals(currency)
                && sameText(previous.getDescription(), description);
        if (!sameRequest) {
            throw new ConflictException("Idempotency key was already used for a different request");
        }
        return toResponse(previous, true);
    }

    private MoneyResponse toResponse(BankingTransaction transaction, boolean replayed) {
        return new MoneyResponse(transaction.getId(), transaction.getReference(), transaction.getStatus().name(),
                transaction.getAmount(), transaction.getCurrency(), replayed);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BusinessRuleException("Amount must have no more than two decimal places");
        }
    }

    private String createAccountNumber() {
        // The unique database constraint is the final protection against duplicate numbers.
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder number = new StringBuilder("LC");
            for (int i = 0; i < 18; i++) {
                number.append(random.nextInt(10));
            }
            if (!accounts.existsByAccountNumber(number.toString())) {
                return number.toString();
            }
        }
        throw new DataIntegrityViolationException("Could not allocate an account number");
    }

    private static boolean sameText(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
