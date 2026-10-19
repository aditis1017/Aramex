package com.customcharge.persistencelayer.calculatorhelper;

import com.customcharge.persistencelayer.config.datasource.DataSourceConfiguration;
import com.customcharge.persistencelayer.config.jpa.JpaConfiguration;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CalculatorHelperConfiguration.CalculatorHelperProperties.class)
@AutoConfigureAfter({DataSourceConfiguration.class, JpaConfiguration.class})
public class CalculatorHelperConfiguration {

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "mvp.persistence.calculator-helper")
    public static class CalculatorHelperProperties {
        private boolean enabled = false;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mvp.persistence.calculator-helper", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public ChargeLineCopyService chargeLineCopyService(
            ServiceChargeResolver serviceChargeResolver,
            ServiceChargeMappingRepository serviceChargeMappingRepository,
            ExchangeRateService exchangeRateService,
            CurrencyRepository currencyRepository,
            FixedDecimalUtil fixedDecimalUtil) {
        return new ChargeLineCopyService(
                serviceChargeResolver,
                serviceChargeMappingRepository,
                exchangeRateService,
                currencyRepository,
                fixedDecimalUtil);
    }
}
