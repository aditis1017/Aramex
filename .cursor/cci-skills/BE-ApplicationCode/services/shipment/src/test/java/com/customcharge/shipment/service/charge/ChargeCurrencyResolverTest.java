package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChargeCurrencyResolverTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private ChargeCurrencyResolver resolver;

    @Test
    void resolve_shouldReturnNull_whenInputsMissing() {
        assertThat(resolver.resolve(null, "ECD")).isNull();
        assertThat(resolver.resolve(new Shipment(), null)).isNull();
        assertThat(resolver.resolve(new Shipment(), " ")).isNull();
        verifyNoInteractions(currencyRepository);
    }

    @Test
    void resolve_shouldReturnDestinationCurrency_forEcd() {
        ShipmentCurrencyContextFactory factory = new ShipmentCurrencyContextFactory(currencyRepository);
        ChargeCurrencyResolver localResolver = new ChargeCurrencyResolver(factory);

        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-1");
        shipment.setConsigneeCountryCode("SA");
        Country country = new Country();
        country.setCode("SA");
        Currency currency = new Currency();
        currency.setCurrencyCode("SAR");
        currency.setCountry(country);
        org.mockito.Mockito.when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue("SA"))
                .thenReturn(Optional.of(currency));

        assertThat(localResolver.resolve(shipment, "ECD")).isEqualTo("SAR");
    }
}
