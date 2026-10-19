package com.customcharge.dashboard.rbac;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ccfe.route-access.cookie")
public class RouteAccessCookieProperties {

    public static final String DEFAULT_COOKIE_NAME = "ccfe_route_access";

    private String name = DEFAULT_COOKIE_NAME;
    private String path = "/";
    private int maxAgeDays = 7;
    private boolean secure = true;
    private String sameSite = "Strict";
}
