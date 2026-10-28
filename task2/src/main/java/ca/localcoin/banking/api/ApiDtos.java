package ca.localcoin.banking.api;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * These are request/response objects, not JPA entities. Keeping the HTTP contract
 * separate from the database makes it easier to change one without changing the other.
 */
public final class ApiDtos {
    private ApiDtos() { }

    public static class OpenAccountRequest {
        @NotBlank @Size(max = 100) public String firstName;
        @Size(max = 100) public String middleName;
        @NotBlank @Size(max = 100) public String lastName;
        @NotNull @Past public LocalDate dateOfBirth;
        @Email @Size(max = 254) public String email;
        @Size(max = 20) public String phone;
        @NotBlank @Size(max = 160) public String addressLine1;
        @Size(max = 160) public String addressLine2;
        @NotBlank @Size(max = 80) public String city;
        @Size(max = 80) public String provinceState;
        @NotBlank @Size(max = 20) public String postalCode;
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") public String countryCode;
        @NotNull @Positive public Long branchId;
        @NotBlank @Pattern(regexp = "[A-Z]{3}") public String currency;
    }

    public static class OpenAccountResponse {
        public Long accountId;
        public String accountNumber;
        public String status;
        public OpenAccountResponse(Long accountId, String accountNumber, String status) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.status = status;
        }
    }

    public static class AccountResponse {
        public Long accountId;
        public String accountNumber;
        public String status;
        public String currency;
        public Long branchId;
        public String holderName;
        public LocalDate dateOfBirth;
        public String email;
        public BigDecimal availableBalance;
        public BigDecimal ledgerBalance;

        public AccountResponse(Long accountId, String accountNumber, String status, String currency,
                               Long branchId, String holderName, LocalDate dateOfBirth, String email,
                               BigDecimal availableBalance, BigDecimal ledgerBalance) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.status = status;
            this.currency = currency;
            this.branchId = branchId;
            this.holderName = holderName;
            this.dateOfBirth = dateOfBirth;
            this.email = email;
            this.availableBalance = availableBalance;
            this.ledgerBalance = ledgerBalance;
        }
    }

    public static class MoneyRequest {
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
        public BigDecimal amount;
        @NotBlank @Pattern(regexp = "[A-Z]{3}") public String currency;
        @Size(max = 255) public String description;
        @NotBlank @Size(max = 100) public String idempotencyKey;
    }

    public static class MoneyResponse {
        public Long transactionId;
        public String transactionReference;
        public String status;
        public BigDecimal amount;
        public String currency;
        public boolean replayed;

        public MoneyResponse(Long transactionId, String transactionReference, String status,
                             BigDecimal amount, String currency, boolean replayed) {
            this.transactionId = transactionId;
            this.transactionReference = transactionReference;
            this.status = status;
            this.amount = amount;
            this.currency = currency;
            this.replayed = replayed;
        }
    }
}
