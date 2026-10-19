package com.customcharge.persistencelayer.config.datasource;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "mvp.persistence.datasource")
public class DatasourceProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName = "org.postgresql.Driver";
    private int maximumPoolSize = 10;
    private int minimumIdle = 2;
    private String poolName = "MVPersistencePool";

    private Routing routing = new Routing();

    private Pool write = new Pool();

    private Pool read = new Pool();

    @Setter
    @Getter
    public static class Routing {
        private boolean enabled = false;
        /**
         * When true and no {@code read.url} is configured, read-only transactions use the write pool.
         * Does not auto-fallback when a separate read URL is configured but unavailable.
         */
        private boolean fallbackToWrite = true;
    }

    @Setter
    @Getter
    public static class Pool {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private Integer maximumPoolSize;
        private Integer minimumIdle;
        private String poolName;
    }
}
