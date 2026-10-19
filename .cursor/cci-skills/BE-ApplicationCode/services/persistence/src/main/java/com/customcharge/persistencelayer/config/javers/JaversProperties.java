package com.customcharge.persistencelayer.config.javers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.javers")
public class JaversProperties {
    private boolean enabled = false;
    private Async async = new Async();

    @Getter
    @Setter
    public static class Async {
        private boolean enabled = false;
        private int poolSize = 4;
    }
}
