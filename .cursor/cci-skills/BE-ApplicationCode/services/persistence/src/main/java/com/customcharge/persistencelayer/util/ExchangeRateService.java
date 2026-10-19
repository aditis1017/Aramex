package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ExchangeRateRequest;
import com.customcharge.persistencelayer.dto.ExchangeRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "mvp.persistence.exchange-rate", name = "url")
public class ExchangeRateService {

    @Value("${mvp.persistence.exchange-rate.url:http://devbe01.aramex.com/corp.shippingapi.coredata.v1/api/DataUtilities/GetExchangeRate}")
    private String exchangeRateUrl;

    @Value("${mvp.persistence.exchange-rate.cache-ttl:2m}")
    private Duration cacheTtl;

    private static final String PREFIX = "ExchangeRate";
    private static final double UNIT_AMOUNT = 1.0;

    private final ObjectProvider<CacheService> cacheService;
    private final RestClient restClient = RestClient.create();

    public ExchangeRateService(ObjectProvider<CacheService> cacheService) {
        this.cacheService = cacheService;
    }

    public CurrencyConversionResult convertAmount(Double amount, String fromCurrency, String toCurrency) {
        return convertAmount(amount, fromCurrency, toCurrency, exchangeRateUrl, null);
    }

    public CurrencyConversionResult convertAmount(Double amount, String fromCurrency, String toCurrency, String apiUrl) {
        return convertAmount(amount, fromCurrency, toCurrency, apiUrl, null);
    }

    public CurrencyConversionResult convertAmount(
            Double amount,
            String fromCurrency,
            String toCurrency,
            String apiUrl,
            String keyPrefix) {
        if (amount == null) {
            return null;
        }

        String from = normalizeCurrency(fromCurrency);
        String to = normalizeCurrency(toCurrency);
        if (from == null || to == null || from.equals(to)) {
            return new CurrencyConversionResult(amount, 1.0);
        }

        String url = apiUrl == null || apiUrl.isBlank() ? exchangeRateUrl : apiUrl;

        try {
            double rate = resolveRate(from, to, url, keyPrefix);
            return new CurrencyConversionResult(amount * rate, rate);
        } catch (Exception ex) {
            log.warn(
                    "Exchange rate unavailable for {} -> {}; returning original amount. reason={}",
                    from,
                    to,
                    ex.getMessage());
            log.debug("Exchange rate failure detail for {} -> {}", from, to, ex);
            return new CurrencyConversionResult(amount, 1.0);
        }
    }

    public static String rateKey(String fromCurrency, String toCurrency) {
        return rateKey(null, fromCurrency, toCurrency);
    }

    public static String rateKey(String keyPrefix, String fromCurrency, String toCurrency) {
        String prefix = keyPrefix != null && !keyPrefix.isBlank() ? keyPrefix.strip() : PREFIX;
        return prefix + ":" + fromCurrency + ":" + toCurrency;
    }

    public static String normalizeCurrency(String currency) {
        if (currency == null) {
            return null;
        }
        String trimmed = currency.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private double resolveRate(String from, String to, String apiUrl, String keyPrefix) {
        String cacheKey = rateKey(keyPrefix, from, to);
        Optional<Double> cached = readCachedRate(cacheKey);
        if (cached.isPresent()) {
            log.debug("Exchange rate cache hit key={} rate={}", cacheKey, cached.get());
            return cached.get();
        }

        double rate = fetchRateFromApi(from, to, apiUrl);
        writeCachedRate(cacheKey, rate);
        return rate;
    }

    private Optional<Double> readCachedRate(String cacheKey) {
        CacheService cache = cacheService.getIfAvailable();
        if (cache == null) {
            return Optional.empty();
        }
        try {
            return cache.get(cacheKey, Double.class);
        } catch (RuntimeException ex) {
            log.warn("Failed to read exchange rate from cache key={}: {}", cacheKey, ex.getMessage());
            log.debug("Exchange rate cache read failure", ex);
            return Optional.empty();
        }
    }

    private void writeCachedRate(String cacheKey, double rate) {
        CacheService cache = cacheService.getIfAvailable();
        if (cache == null) {
            return;
        }
        try {
            cache.put(cacheKey, rate, cacheTtl);
            log.debug("Cached exchange rate key={} rate={} ttl={}", cacheKey, rate, cacheTtl);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache exchange rate key={}: {}", cacheKey, ex.getMessage());
            log.debug("Exchange rate cache write failure", ex);
        }
    }

    private double fetchRateFromApi(String from, String to, String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("Exchange rate API URL is not configured");
        }

        ExchangeRateRequest request = new ExchangeRateRequest(UNIT_AMOUNT, from, to);
        log.debug("Fetching exchange rate from API {} ({} -> {})", apiUrl, from, to);
        try {
            ExchangeRateResponse response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExchangeRateResponse.class);

            Double convertedAmount = response != null ? response.convertedAmount() : null;
            if (convertedAmount == null || convertedAmount <= 0 || !Double.isFinite(convertedAmount)) {
                throw new IllegalStateException("Invalid convertedAmount from exchange API");
            }
            log.info("Exchange rate fetched {} -> {} rate={}", from, to, convertedAmount);
            return convertedAmount;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Exchange rate HTTP " + ex.getStatusCode().value() + ": " + ex.getStatusText(), ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Exchange rate call failed: " + ex.getMessage(), ex);
        }
    }
}
