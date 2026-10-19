package com.customcharge.persistencelayer.logging;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class LoggingAutoConfiguration {

    @Bean
    public FilterRegistrationBean<Filter> traceIdFilter() {

        FilterRegistrationBean<Filter> bean =
                new FilterRegistrationBean<>();

        bean.setFilter(new TraceIdFilter());
        bean.addUrlPatterns("/*");

        return bean;
    }
}
