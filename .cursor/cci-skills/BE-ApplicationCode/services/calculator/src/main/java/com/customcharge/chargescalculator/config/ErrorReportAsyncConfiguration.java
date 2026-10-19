package com.customcharge.chargescalculator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ErrorReportAsyncConfiguration {

    @Bean(name = "errorReportTaskExecutor")
    public Executor errorReportTaskExecutor(CustomChargeProperties customChargeProperties) {
        CustomChargeProperties.ErrorReport errorReport = customChargeProperties.getErrorReport();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(errorReport.getAsyncCorePoolSize());
        executor.setMaxPoolSize(errorReport.getAsyncMaxPoolSize());
        executor.setQueueCapacity(errorReport.getAsyncQueueCapacity());
        executor.setThreadNamePrefix("error-report-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
