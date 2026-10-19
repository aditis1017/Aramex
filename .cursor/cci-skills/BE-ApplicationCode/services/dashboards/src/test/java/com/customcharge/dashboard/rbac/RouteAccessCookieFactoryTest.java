package com.customcharge.dashboard.rbac;

import com.customcharge.dashboard.dto.RolePermissionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

class RouteAccessCookieFactoryTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void buildPayloadMapsPermissionCodesToModuleGrants() {
    List<RolePermissionDTO> permissions = List.of(
        RolePermissionDTO.builder()
            .moduleName("Customs")
            .permissionCodes(Set.of("CUSTOMS_VIEW", "CUSTOMS_CREATE"))
            .build(),
        RolePermissionDTO.builder()
            .moduleName("AR Management")
            .permissionCodes(Set.of("AR_MANAGEMENT_VIEW"))
            .build());

    Map<String, Object> payload = RouteAccessCookieFactory.buildPayload(permissions);

    assertTrue((Boolean) payload.get("ready"));
    assertFalse((Boolean) payload.get("superAdmin"));
    @SuppressWarnings("unchecked")
    Map<String, List<String>> grants = (Map<String, List<String>>) payload.get("moduleGrants");
    assertThat(grants.get("CUSTOMS")).containsExactlyInAnyOrder("VIEW", "CREATE");
    assertEquals(List.of("VIEW"), grants.get("AR_MANAGEMENT"));
  }

  @Test
  void buildPayloadDoesNotInferSuperAdminFromRoleNaming() {
    List<RolePermissionDTO> permissions = List.of();

    Map<String, Object> payload = RouteAccessCookieFactory.buildPayload(permissions);
    assertFalse((Boolean) payload.get("superAdmin"));
  }

  @Test
  void buildCookieJsonProducesParseablePayload() throws Exception {
    List<RolePermissionDTO> permissions = List.of(
        RolePermissionDTO.builder()
            .moduleName("Customs")
            .permissionCodes(Set.of("CUSTOMS_VIEW"))
            .build());

    String json = RouteAccessCookieFactory.buildCookieJson(permissions, objectMapper);
    Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
    assertEquals(true, parsed.get("ready"));
    assertEquals(false, parsed.get("superAdmin"));
  }

  @Test
  void buildPayloadShouldInferModuleFromModuleNameWhenCodesMissing() {
    List<RolePermissionDTO> permissions = List.of(
        RolePermissionDTO.builder()
            .moduleName("AR Management")
            .permissionCodes(Set.of())
            .build());

    Map<String, Object> payload = RouteAccessCookieFactory.buildPayload(permissions);

    @SuppressWarnings("unchecked")
    Map<String, List<String>> grants = (Map<String, List<String>>) payload.get("moduleGrants");
    assertThat(grants).containsKey("AR_MANAGEMENT");
    assertThat(grants.get("AR_MANAGEMENT")).isEmpty();
  }

  @Test
  void buildPayloadShouldSkipNullPermissionsAndInvalidCodes() {
    List<RolePermissionDTO> permissions = new java.util.ArrayList<>();
    permissions.add(null);
    permissions.add(RolePermissionDTO.builder()
        .moduleName(" ")
        .permissionCodes(Set.of("INVALID", "CUSTOMS_UNKNOWN"))
        .build());

    Map<String, Object> payload = RouteAccessCookieFactory.buildPayload(permissions);

    @SuppressWarnings("unchecked")
    Map<String, List<String>> grants = (Map<String, List<String>>) payload.get("moduleGrants");
    assertThat(grants).isEmpty();
  }

  @Test
  void extractPrefixAndActionHelpersShouldHandleEdgeCases() {
    assertNull(RouteAccessCookieFactory.extractPrefixFromFullPermissionCode(null));
    assertNull(RouteAccessCookieFactory.extractActionFromFullPermissionCode(" "));
    assertEquals("GLOBAL_SETTINGS", RouteAccessCookieFactory.extractPrefixFromFullPermissionCode("GLOBAL_SETTINGS_VIEW"));
    assertEquals("VIEW", RouteAccessCookieFactory.extractActionFromFullPermissionCode("CUSTOMS_VIEW"));
    assertNull(RouteAccessCookieFactory.extractActionFromFullPermissionCode("CUSTOMS"));
    assertEquals("B2B_KEYWORDS_MANAGER", RouteAccessCookieFactory.inferPermissionCodePrefixFromModuleName("B2B Keywords Manager"));
  }
}
