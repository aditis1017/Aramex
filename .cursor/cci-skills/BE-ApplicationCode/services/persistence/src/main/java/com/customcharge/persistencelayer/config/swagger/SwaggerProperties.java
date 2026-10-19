package com.customcharge.persistencelayer.config.swagger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mvp.persistence.swagger")
@Getter
@Setter
public class SwaggerProperties {

    private boolean enabled = true;

    private String title = "API Documentation";
    private String description = "API Description";
    private String version = "1.0.0";

    private String contactName;
    private String contactEmail;
    private String contactUrl;
    private String serverUrl;

}
