package com.customcharge.dashboard.rbac;

import com.customcharge.dashboard.dto.RoleDTO;
import com.customcharge.dashboard.dto.RolePermissionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RouteAccessCookieServiceTest {

    @Mock
    private HttpServletResponse response;

    private RouteAccessCookieService service;

    @BeforeEach
    void setUp() {
        RouteAccessCookieProperties properties = new RouteAccessCookieProperties();
        properties.setName("ccfe_route_access");
        properties.setPath("dashboard");
        properties.setMaxAgeDays(3);
        properties.setSecure(false);
        properties.setSameSite("Lax");
        service = new RouteAccessCookieService(properties, new ObjectMapper());
    }

    @Test
    void attachRouteAccessCookie_shouldIgnoreNullResponse() {
        RoleDTO role = new RoleDTO();
        role.setPermissions(List.of());

        service.attachRouteAccessCookie(null, role);

        verifyNoInteractions(response);
    }

    @Test
    void attachRouteAccessCookie_shouldIgnoreNullRole() {
        service.attachRouteAccessCookie(response, null);

        verifyNoInteractions(response);
    }

    @Test
    void attachRouteAccessCookie_shouldSetCookieHeader() {
        RoleDTO role = new RoleDTO();
        role.setPermissions(List.of(
                RolePermissionDTO.builder()
                        .moduleName("Customs")
                        .permissionCodes(Set.of("CUSTOMS_VIEW"))
                        .build()));

        service.attachRouteAccessCookie(response, role);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(nameCaptor.capture(), valueCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo(HttpHeaders.SET_COOKIE);
        assertThat(valueCaptor.getValue()).contains("ccfe_route_access=");
        assertThat(valueCaptor.getValue()).contains("Path=/dashboard");
        assertThat(valueCaptor.getValue()).contains("SameSite=Lax");
    }

    @Test
    void attachRouteAccessCookie_shouldDefaultBlankPathToRoot() {
        RouteAccessCookieProperties properties = new RouteAccessCookieProperties();
        properties.setPath(" ");
        RouteAccessCookieService pathService = new RouteAccessCookieService(properties, new ObjectMapper());

        RoleDTO role = new RoleDTO();
        role.setPermissions(List.of());

        pathService.attachRouteAccessCookie(response, role);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(org.mockito.ArgumentMatchers.eq(HttpHeaders.SET_COOKIE), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).contains("Path=/");
    }
}
