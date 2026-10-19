package com.customcharge.persistencelayer.config.drools;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.drools")
public class DroolsProperties {
    private boolean enabled = false;
}
