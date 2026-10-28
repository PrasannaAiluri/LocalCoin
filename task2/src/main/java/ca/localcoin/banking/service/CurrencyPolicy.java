package ca.localcoin.banking.service;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class CurrencyPolicy {
    // Each currency enabled here has two minor-unit decimal places.
    // Add currencies only after confirming the bank's precision and rounding rules.
    private final Set<String> supported = new HashSet<String>(Arrays.asList(
            "CAD", "USD", "EUR", "GBP", "AUD", "NZD", "CHF", "SGD"));

    public String requireSupported(String currency) {
        if (currency == null) {
            throw new BusinessRuleException("Currency is required");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!supported.contains(normalized)) {
            throw new BusinessRuleException("Currency is not supported by this application");
        }
        return normalized;
    }
}
