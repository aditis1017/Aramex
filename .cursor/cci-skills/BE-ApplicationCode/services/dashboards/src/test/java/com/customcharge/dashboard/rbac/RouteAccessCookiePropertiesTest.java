package com.customcharge.dashboard.rbac;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteAccessCookiePropertiesTest {

    @Test
    void shouldExposeDefaultsAndAllowOverrides() {
        RouteAccessCookieProperties properties = new RouteAccessCookieProperties();

        assertThat(properties.getName()).isEqualTo(RouteAccessCookieProperties.DEFAULT_COOKIE_NAME);
        assertThat(properties.getPath()).isEqualTo("/");
        assertThat(properties.getMaxAgeDays()).isEqualTo(7);
        assertThat(properties.isSecure()).isTrue();
        assertThat(properties.getSameSite()).isEqualTo("Strict");

        properties.setName("custom_cookie");
        properties.setPath("/app");
        properties.setMaxAgeDays(14);
        properties.setSecure(false);
        properties.setSameSite("None");

        assertThat(properties.getName()).isEqualTo("custom_cookie");
        assertThat(properties.getPath()).isEqualTo("/app");
        assertThat(properties.getMaxAgeDays()).isEqualTo(14);
        assertThat(properties.isSecure()).isFalse();
        assertThat(properties.getSameSite()).isEqualTo("None");
    }
}
