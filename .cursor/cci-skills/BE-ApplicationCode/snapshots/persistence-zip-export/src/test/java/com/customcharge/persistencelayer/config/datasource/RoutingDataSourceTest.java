package com.customcharge.persistencelayer.config.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingDataSourceTest {

    @BeforeEach
    void initTransactionContext() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionContext() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldRouteToWrite_forNonReadOnlyTransaction() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);

        assertThat(exposedRouting(true, true).currentRoute()).isEqualTo(DataSourceRoute.WRITE);
    }

    @Test
    void shouldRouteToRead_forReadOnlyWhenReadPoolConfigured() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);

        assertThat(exposedRouting(true, true).currentRoute()).isEqualTo(DataSourceRoute.READ);
    }

    @Test
    void shouldFallbackToWrite_forReadOnlyWhenReadPoolNotConfigured() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);

        assertThat(exposedRouting(true, false).currentRoute()).isEqualTo(DataSourceRoute.WRITE);
    }

    @Test
    void shouldRouteToRead_whenFallbackDisabledAndReadPoolMissing() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);

        assertThat(exposedRouting(false, false).currentRoute()).isEqualTo(DataSourceRoute.READ);
    }

    private static ExposedRoutingDataSource exposedRouting(boolean fallbackToWrite, boolean readPoolConfigured) {
        return new ExposedRoutingDataSource(fallbackToWrite, readPoolConfigured);
    }

    private static final class ExposedRoutingDataSource extends RoutingDataSource {
        private ExposedRoutingDataSource(boolean fallbackToWrite, boolean readPoolConfigured) {
            super(fallbackToWrite, readPoolConfigured);
        }

        private DataSourceRoute currentRoute() {
            return (DataSourceRoute) determineCurrentLookupKey();
        }
    }
}
