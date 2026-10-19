package com.customcharge.persistencelayer.security.entra;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Registers {@link EntraOidcSecurityProperties} whenever Entra security is enabled (same condition as servlet
 * security beans in {@link EntraOidcWebSecurityConfiguration}).
 */
@AutoConfiguration
@ConditionalOnEntraOidcSecurityEnabled
@EnableConfigurationProperties(EntraOidcSecurityProperties.class)
public class EntraOidcSecurityPropertiesAutoConfiguration {
}
