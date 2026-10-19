package com.customcharge.shipment.service.cif;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Parses CIF amount and currency from HAWB {@code Comment2}.
 * <p>
 * Extended format (3+ tokens): {@code 0 TND 757708 Cash ...} — currency is the 2nd token, amount the 3rd.
 * Short format (2 tokens): {@code 15 TND} — amount is the 1st token, currency the 2nd.
 */
public final class HawbComment2CifParser {

    private HawbComment2CifParser() {
    }

    public record ParsedCif(double amount, String currency) {
    }

    public static Optional<ParsedCif> parse(String comment2) {
        if (!StringUtils.hasText(comment2)) {
            return Optional.empty();
        }
        String[] tokens = comment2.trim().split("\\s+");
        if (tokens.length < 2) {
            return Optional.empty();
        }
        try {
            String currency;
            BigDecimal amount;
            if (tokens.length >= 3) {
                currency = tokens[1].trim();
                amount = new BigDecimal(tokens[2].trim());
            } else {
                amount = new BigDecimal(tokens[0].trim());
                currency = tokens[1].trim();
            }
            if (currency.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ParsedCif(amount.doubleValue(), currency));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
