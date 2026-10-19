package com.customcharge.persistencelayer.config.drools;

import com.customcharge.persistencelayer.rules.DroolsRuleEngine;
import com.customcharge.persistencelayer.rules.RuleEngine;
import org.kie.api.KieServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "mvp.persistence.drools",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ConditionalOnClass(KieServices.class)
@EnableConfigurationProperties(DroolsProperties.class)
public class DroolsConfiguration {

    @Bean
    @ConditionalOnMissingBean(KieServices.class)
    public KieServices kieServices() {
        return KieServices.Factory.get();
    }

    @Bean
    @ConditionalOnMissingBean(RuleEngine.class)
    public RuleEngine ruleEngine(KieServices kieServices) {
        return new DroolsRuleEngine(kieServices);
    }
}
