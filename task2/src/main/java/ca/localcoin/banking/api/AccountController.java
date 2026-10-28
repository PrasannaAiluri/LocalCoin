package ca.localcoin.banking.api;

import ca.localcoin.banking.api.ApiDtos.AccountResponse;
import ca.localcoin.banking.api.ApiDtos.MoneyRequest;
import ca.localcoin.banking.api.ApiDtos.MoneyResponse;
import ca.localcoin.banking.api.ApiDtos.OpenAccountRequest;
import ca.localcoin.banking.api.ApiDtos.OpenAccountResponse;
import ca.localcoin.banking.service.BankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public ResponseEntity<OpenAccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request) {
        OpenAccountResponse response = bankingService.openAccount(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.accountId))
                .body(response);
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable("accountId") Long accountId) {
        return bankingService.getAccount(accountId);
    }

    @PostMapping("/{accountId}/withdrawals")
    public ResponseEntity<MoneyResponse> withdraw(
            @PathVariable("accountId") Long accountId,
            @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(201).body(bankingService.withdraw(accountId, request));
    }
}
