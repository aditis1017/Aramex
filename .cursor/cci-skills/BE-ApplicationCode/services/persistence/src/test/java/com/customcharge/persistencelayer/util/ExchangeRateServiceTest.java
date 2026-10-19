package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;

    @Mock
    private CacheService cacheService;

    private ExchangeRateService exchangeRateService;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        exchangeRateService = new ExchangeRateService(cacheServiceProvider);
        ReflectionTestUtils.setField(
                exchangeRateService,
                "exchangeRateUrl",
                mockWebServer.url("/rate").toString());
        ReflectionTestUtils.setField(exchangeRateService, "cacheTtl", Duration.ofMinutes(2));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void normalizeCurrency_shouldUppercaseAndTrim() {
        assertThat(ExchangeRateService.normalizeCurrency(" usd ")).isEqualTo("USD");
        assertThat(ExchangeRateService.normalizeCurrency(null)).isNull();
        assertThat(ExchangeRateService.normalizeCurrency("  ")).isNull();
    }

    @Test
    void rateKey_shouldBuildExpectedKey() {
        assertThat(ExchangeRateService.rateKey("USD", "EUR")).isEqualTo("ExchangeRate:USD:EUR");
        assertThat(ExchangeRateService.rateKey("Custom", "USD", "EUR")).isEqualTo("Custom:USD:EUR");
    }

    @Test
    void convertAmount_shouldReturnNull_whenAmountNull() {
        assertThat(exchangeRateService.convertAmount(null, "USD", "EUR")).isNull();
    }

    @Test
    void convertAmount_shouldReturnSameAmount_whenCurrenciesMatch() {
        assertThat(exchangeRateService.convertAmount(100.0, "USD", "usd"))
                .isEqualTo(new CurrencyConversionResult(100.0, 1.0));
    }

    @Test
    void convertAmount_shouldUseCachedRate_whenPresent() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("ExchangeRate:USD:EUR", Double.class)).thenReturn(Optional.of(2.0));

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                50.0,
                "USD",
                "EUR",
                "http://exchange.invalid/api/rate",
                null);

        assertThat(result).isEqualTo(new CurrencyConversionResult(100.0, 2.0));
        verify(cacheService).get("ExchangeRate:USD:EUR", Double.class);
    }

    @Test
    void convertAmount_shouldReturnOriginalAmount_whenApiUnavailable() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                75.0,
                "USD",
                "EUR",
                "http://127.0.0.1:1/unreachable",
                null);

        assertThat(result).isEqualTo(new CurrencyConversionResult(75.0, 1.0));
    }

    @Test
    void convertAmount_shouldFetchFromApiAndCacheRate() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "amount": 1.0,
                          "currentCurrencyCode": "USD",
                          "newCurrencyCode": "EUR",
                          "convertedAmount": 0.85
                        }
                        """)
                .addHeader("Content-Type", "application/json"));
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("ExchangeRate:USD:EUR", Double.class)).thenReturn(Optional.empty());

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                100.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString(),
                null);

        assertThat(result).isEqualTo(new CurrencyConversionResult(85.0, 0.85));
        verify(cacheService).put(eq("ExchangeRate:USD:EUR"), eq(0.85), eq(Duration.ofMinutes(2)));
    }

    @Test
    void convertAmount_shouldUseConfiguredCacheTtl() {
        ReflectionTestUtils.setField(exchangeRateService, "cacheTtl", Duration.ofHours(15));
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "amount": 1.0,
                          "currentCurrencyCode": "USD",
                          "newCurrencyCode": "EUR",
                          "convertedAmount": 0.85
                        }
                        """)
                .addHeader("Content-Type", "application/json"));
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("ExchangeRate:USD:EUR", Double.class)).thenReturn(Optional.empty());

        exchangeRateService.convertAmount(
                100.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString(),
                null);

        verify(cacheService).put(eq("ExchangeRate:USD:EUR"), eq(0.85), eq(Duration.ofHours(15)));
    }

    @Test
    void convertAmount_shouldIgnoreCacheReadFailure() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("Custom:USD:EUR", Double.class)).thenThrow(new RuntimeException("redis down"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "amount": 1.0,
                          "currentCurrencyCode": "USD",
                          "newCurrencyCode": "EUR",
                          "convertedAmount": 2.0
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                10.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString(),
                "Custom");

        assertThat(result).isEqualTo(new CurrencyConversionResult(20.0, 2.0));
    }

    @Test
    void convertAmount_threeArgOverload_shouldDelegateToFiveArg() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("ExchangeRate:USD:EUR", Double.class)).thenReturn(Optional.of(1.5));

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                10.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString());

        assertThat(result).isEqualTo(new CurrencyConversionResult(15.0, 1.5));
    }

    @Test
    void convertAmount_twoArgOverload_shouldUseConfiguredUrl() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("ExchangeRate:USD:EUR", Double.class)).thenReturn(Optional.of(2.0));

        CurrencyConversionResult result = exchangeRateService.convertAmount(5.0, "USD", "EUR");

        assertThat(result).isEqualTo(new CurrencyConversionResult(10.0, 2.0));
    }

    @Test
    void convertAmount_shouldReturnOriginalAmount_whenApiReturnsInvalidRate() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "amount": 1.0,
                          "currentCurrencyCode": "USD",
                          "newCurrencyCode": "EUR",
                          "convertedAmount": -1.0
                        }
                        """)
                .addHeader("Content-Type", "application/json"));
        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                50.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString(),
                null);

        assertThat(result).isEqualTo(new CurrencyConversionResult(50.0, 1.0));
    }

    @Test
    void convertAmount_shouldReturnOriginalAmount_whenApiReturnsHttpError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                25.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString(),
                null);

        assertThat(result).isEqualTo(new CurrencyConversionResult(25.0, 1.0));
    }

    @Test
    void convertAmount_shouldIgnoreCacheWriteFailure() {
        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get("ExchangeRate:USD:EUR", Double.class)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("redis down"))
                .when(cacheService).put(eq("ExchangeRate:USD:EUR"), eq(0.5), any());
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "amount": 1.0,
                          "currentCurrencyCode": "USD",
                          "newCurrencyCode": "EUR",
                          "convertedAmount": 0.5
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        CurrencyConversionResult result = exchangeRateService.convertAmount(
                20.0,
                "USD",
                "EUR",
                mockWebServer.url("/rate").toString(),
                null);

        assertThat(result).isEqualTo(new CurrencyConversionResult(10.0, 0.5));
    }
}
